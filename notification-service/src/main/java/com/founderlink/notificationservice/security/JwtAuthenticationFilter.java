package com.founderlink.notificationservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
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
      throws ServletException, java.io.IOException {
    UsernamePasswordAuthenticationToken authentication = fromJwt(request);
    if (authentication == null) {
      authentication = fromGatewayHeaders(request);
    }
    // Always prefer gateway/JWT-derived identity when present.
    if (authentication != null) {
      SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    filterChain.doFilter(request, response);
  }

  private UsernamePasswordAuthenticationToken fromGatewayHeaders(HttpServletRequest request) {
    String userId = request.getHeader("X-User-Id");
    String roles = request.getHeader("X-Roles");
    if (!StringUtils.hasText(userId)) {
      return null;
    }
    return new UsernamePasswordAuthenticationToken(userId, null, parseRolesClaim(roles));
  }

  private UsernamePasswordAuthenticationToken fromJwt(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    String token = authHeader.substring(7);
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    Claims claims = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
    Object uid = claims.get("userId");
    String principal = uid == null ? null : String.valueOf(uid);
    if (!StringUtils.hasText(principal) || "null".equalsIgnoreCase(principal)) {
      principal = claims.getSubject();
    }
    if (!StringUtils.hasText(principal)) {
      return null;
    }
    return new UsernamePasswordAuthenticationToken(
        principal, null, parseRolesClaim(claims.get("roles")));
  }

  private List<SimpleGrantedAuthority> parseRolesClaim(Object rolesClaim) {
    if (rolesClaim == null) {
      return List.of();
    }

    if (rolesClaim instanceof Collection<?> collection) {
      return collection.stream()
          .map(String::valueOf)
          .map(this::normalizeRole)
          .filter(StringUtils::hasText)
          .map(SimpleGrantedAuthority::new)
          .collect(Collectors.toList());
    }

    String rolesText = String.valueOf(rolesClaim).trim();
    if (!StringUtils.hasText(rolesText) || "null".equalsIgnoreCase(rolesText)) {
      return List.of();
    }
    if (rolesText.startsWith("[") && rolesText.endsWith("]") && rolesText.length() >= 2) {
      rolesText = rolesText.substring(1, rolesText.length() - 1);
    }

    return Arrays.stream(rolesText.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(this::normalizeRole)
        .filter(StringUtils::hasText)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }

  private String normalizeRole(String role) {
    if (!StringUtils.hasText(role)) {
      return null;
    }
    String normalized = role.trim().replace("\"", "").replace("'", "");
    if (normalized.startsWith("ROLE_")) {
      return normalized.toUpperCase();
    }
    return "ROLE_" + normalized.toUpperCase();
  }
}
