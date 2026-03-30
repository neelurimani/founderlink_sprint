package com.founderlink.apigateway.security;

import com.founderlink.apigateway.config.GatewayProperties;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 20)
public class RateLimitFilter implements WebFilter {

  private final GatewayProperties gatewayProperties;
  private final ConcurrentHashMap<String, WindowCounter> counters = new ConcurrentHashMap<>();

  public RateLimitFilter(GatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
    int maxRequests = gatewayProperties.getRateLimit().getRequestsPerMinute();
    String apiKeyHeader = gatewayProperties.getSecurity().getApiKeyHeader();
    String apiKey = exchange.getRequest().getHeaders().getFirst(apiKeyHeader);
    String clientIp =
        Objects.requireNonNullElse(exchange.getRequest().getRemoteAddress(), "").toString();
    String clientId = StringUtils.hasText(apiKey) ? apiKey : clientIp;

    long currentWindow = Instant.now().getEpochSecond() / 60;
    WindowCounter windowCounter =
        counters.compute(
            clientId,
            (id, existing) -> {
              if (existing == null || existing.window() != currentWindow) {
                return new WindowCounter(currentWindow, new AtomicInteger(1));
              }
              existing.counter().incrementAndGet();
              return existing;
            });

    if (windowCounter.counter().get() > maxRequests) {
      exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
      exchange.getResponse().getHeaders().add("Retry-After", "60");
      byte[] payload = "Rate limit exceeded".getBytes(StandardCharsets.UTF_8);
      return exchange
          .getResponse()
          .writeWith(Mono.just(exchange.getResponse().bufferFactory().wrap(payload)));
    }

    return chain.filter(exchange);
  }

  private record WindowCounter(long window, AtomicInteger counter) {}
}
