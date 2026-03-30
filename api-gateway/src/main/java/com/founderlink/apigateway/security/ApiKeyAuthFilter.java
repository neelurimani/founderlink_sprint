package com.founderlink.apigateway.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.apigateway.config.GatewayProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class ApiKeyAuthFilter implements WebFilter {

  private final GatewayProperties gatewayProperties;
  private final ObjectMapper objectMapper;
  private final AntPathMatcher pathMatcher = new AntPathMatcher();

  public ApiKeyAuthFilter(GatewayProperties gatewayProperties, ObjectMapper objectMapper) {
    this.gatewayProperties = gatewayProperties;
    this.objectMapper = objectMapper;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    ServerHttpRequest request = exchange.getRequest();
    String path = request.getPath().value();
    if (isUnsecured(path)) {
      return chain.filter(exchange);
    }
    if (isAnonymousPermitted(request)) {
      return chain.filter(exchange);
    }

    GatewayProperties.Security security = gatewayProperties.getSecurity();
    if (security.isJwtEnabled()) {
      try {
        Claims claims = parseJwt(request);
        ServerHttpRequest updatedRequest =
            request
                .mutate()
                .header("X-User-Id", getClaimValue(claims, "userId", claims.getSubject()))
                .header("X-Roles", getClaimValue(claims, "roles", ""))
                .build();
        return chain.filter(exchange.mutate().request(updatedRequest).build());
      } catch (Exception ignored) {
        // Fallback to API key path if enabled, otherwise reject.
      }
    }

    if (!security.isJwtEnabled() && !security.isApiKeyEnabled()) {
      return chain.filter(exchange);
    }

    if (!security.isApiKeyEnabled()) {
      return unauthorized(exchange, "Missing or invalid JWT token");
    }

    String header = gatewayProperties.getSecurity().getApiKeyHeader();
    String requestApiKey = request.getHeaders().getFirst(header);

    List<String> validApiKeys = gatewayProperties.getSecurity().getValidApiKeys();
    if (validApiKeys.isEmpty()) {
      return chain.filter(exchange);
    }

    if (!StringUtils.hasText(requestApiKey) || !validApiKeys.contains(requestApiKey)) {
      return unauthorized(exchange, "Missing or invalid API key");
    }

    return chain.filter(exchange);
  }

  private Claims parseJwt(ServerHttpRequest request) {
    String authHeader =
        request.getHeaders().getFirst(gatewayProperties.getSecurity().getAuthHeader());
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      throw new IllegalArgumentException("Missing bearer token");
    }
    String token = authHeader.substring(7);
    SecretKey key =
        Keys.hmacShaKeyFor(
            gatewayProperties.getSecurity().getJwtSecret().getBytes(StandardCharsets.UTF_8));
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private String getClaimValue(Claims claims, String key, String fallback) {
    Object value = claims.get(key);
    return value == null ? fallback : String.valueOf(value);
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange, String message) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
    try {
      var root = objectMapper.createObjectNode();
      root.put("timestamp", Instant.now().toString());
      root.put("status", HttpStatus.UNAUTHORIZED.value());
      root.put("error", HttpStatus.UNAUTHORIZED.getReasonPhrase());
      root.put("message", message);
      root.put("path", exchange.getRequest().getPath().value());
      byte[] payload = objectMapper.writeValueAsBytes(root);
      return exchange
          .getResponse()
          .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    } catch (Exception ex) {
      byte[] payload = message.getBytes(StandardCharsets.UTF_8);
      return exchange
          .getResponse()
          .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }
  }

  /** Read-only catalog routes: must match startup-service {@code permitAll} GET listing. */
  private boolean isAnonymousPermitted(ServerHttpRequest request) {
    if (!HttpMethod.GET.equals(request.getMethod())) {
      return false;
    }
    String path = request.getPath().value();
    return pathMatcher.match("/api/startups", path) || pathMatcher.match("/api/startups/**", path);
  }

  private boolean isUnsecured(String path) {
    return gatewayProperties.getSecurity().getUnsecuredPaths().stream()
        .anyMatch(pattern -> pathMatcher.match(pattern, path));
  }
}
