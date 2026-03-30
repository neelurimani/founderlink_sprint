package com.founderlink.startupservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private static final Logger log = LoggerFactory.getLogger(JwtAuthenticationFilter.class);

  @Value("${security.jwt.secret:${JWT_SECRET:founderlink-dev-secret-key-change-me-123456}}")
  private String jwtSecret;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // Prefer Authorization Bearer over X-User-Id / X-Roles so principal and roles match the signed
    // JWT.
    UsernamePasswordAuthenticationToken authentication = safeFromJwt(request);
    if (authentication == null) {
      authentication = fromGatewayHeaders(request);
    }
    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
      log.debug(
          "Authenticated request principal={} authorities={}",
          authentication.getPrincipal(),
          authentication.getAuthorities());
    } else {
      log.trace("No JWT or gateway headers for {}", request.getRequestURI());
    }
    filterChain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken safeFromJwt(HttpServletRequest request) {
    try {
      return fromJwt(request);
    } catch (Exception ex) {
      log.warn("JWT validation failed for {}: {}", request.getRequestURI(), ex.getMessage());
      return null;
    }
  }

  private UsernamePasswordAuthenticationToken fromGatewayHeaders(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    String roles = request.getHeader("X-Roles");
    if (!StringUtils.hasText(userId)) {
      return null;
    }
    List<SimpleGrantedAuthority> authorities = parseRoles(roles);
    return new UsernamePasswordAuthenticationToken(userId, null, authorities);
  }

  private UsernamePasswordAuthenticationToken fromJwt(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring(7);
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    Object userId = claims.get("userId");
    String principal = userId == null ? null : String.valueOf(userId);
    if (!StringUtils.hasText(principal) || "null".equalsIgnoreCase(principal)) {
      principal = claims.getSubject();
    }
    List<SimpleGrantedAuthority> authorities = parseRoles(String.valueOf(claims.get("roles")));
    return new UsernamePasswordAuthenticationToken(principal, null, authorities);
  }

  private List<SimpleGrantedAuthority> parseRoles(String roles) {
    if (!StringUtils.hasText(roles)) {
      return List.of();
    }
    String cleaned = roles.trim();
    if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
      cleaned = cleaned.substring(1, cleaned.length() - 1);
    }
    return Arrays.stream(cleaned.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(r -> r.replace("\"", "").replace("'", "").trim())
        .filter(StringUtils::hasText)
        .map(String::toUpperCase)
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
