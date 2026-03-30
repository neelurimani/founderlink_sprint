package com.founderlink.teamservice.security;

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
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  @Value("${security.jwt.secret:${JWT_SECRET:founderlink-dev-secret-key-change-me-123456}}")
  private String jwtSecret;

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    UsernamePasswordAuthenticationToken authentication = safeFromJwt(request);
    if (authentication == null) {
      authentication = fromGatewayHeaders(request);
    }
    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }
    filterChain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken safeFromJwt(HttpServletRequest request) {
    try {
      return fromJwt(request);
    } catch (Exception ignored) {
      return null;
    }
  }

  private UsernamePasswordAuthenticationToken fromGatewayHeaders(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    String roles = request.getHeader("X-Roles");
    if (!StringUtils.hasText(userId)) {
      return null;
    }
    return new UsernamePasswordAuthenticationToken(
        userId, null, parseRoles(normalizeRolesRaw(roles)));
  }

  private UsernamePasswordAuthenticationToken fromJwt(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring(7);
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    Object userIdClaim = claims.get("userId");
    String principal = userIdClaim == null ? null : String.valueOf(userIdClaim);
    if (!StringUtils.hasText(principal) || "null".equalsIgnoreCase(principal)) {
      principal = claims.getSubject();
    }
    return new UsernamePasswordAuthenticationToken(
        principal, null, parseRoles(extractRolesString(claims)));
  }

  /**
   * Avoid {@code String.valueOf(null) => "null"} and support String or Collection claims from JWT /
   * gateways.
   */
  private static String extractRolesString(Claims claims) {
    Object raw = claims.get("roles");
    if (raw == null) {
      return "";
    }
    if (raw instanceof String s) {
      return normalizeRolesRaw(s);
    }
    if (raw instanceof Collection<?> c) {
      return c.stream()
          .filter(Objects::nonNull)
          .map(Object::toString)
          .map(String::trim)
          .filter(StringUtils::hasText)
          .collect(Collectors.joining(","));
    }
    return normalizeRolesRaw(String.valueOf(raw));
  }

  private static String normalizeRolesRaw(String raw) {
    if (raw == null) {
      return "";
    }
    String t = raw.trim();
    if (!StringUtils.hasText(t) || "null".equalsIgnoreCase(t)) {
      return "";
    }
    return t;
  }

  private List<SimpleGrantedAuthority> parseRoles(String roles) {
    if (!StringUtils.hasText(roles)) {
      return List.of();
    }
    String rolesText = roles.trim();
    if (rolesText.startsWith("[") && rolesText.endsWith("]") && rolesText.length() >= 2) {
      rolesText = rolesText.substring(1, rolesText.length() - 1);
    }
    rolesText = rolesText.replace("\"", "").replace("'", "").trim();

    return Arrays.stream(rolesText.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(r -> r.replace("\"", "").replace("'", "").trim())
        .map(
            r -> {
              String upper = r.toUpperCase();
              return upper.startsWith("ROLE_") ? upper : "ROLE_" + upper;
            })
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
