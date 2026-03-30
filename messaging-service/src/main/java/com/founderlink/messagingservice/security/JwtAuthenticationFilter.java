package com.founderlink.messagingservice.security;

import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtSupport jwtSupport;

  public JwtAuthenticationFilter(JwtSupport jwtSupport) {
    this.jwtSupport = jwtSupport;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    UsernamePasswordAuthenticationToken authentication = fromJwt(request);
    if (authentication == null) {
      authentication = fromGatewayHeaders(request);
    }
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
    return new UsernamePasswordAuthenticationToken(userId, null, parseRoles(roles));
  }

  private UsernamePasswordAuthenticationToken fromJwt(HttpServletRequest request) {
    String token = jwtSupport.extractBearerToken(request.getHeader("Authorization"));
    if (!StringUtils.hasText(token)) {
      return null;
    }
    Claims claims = jwtSupport.parseToken(token);
    String principal = claims.get("userId", String.class);
    if (!StringUtils.hasText(principal)) {
      principal = claims.getSubject();
    }
    return new UsernamePasswordAuthenticationToken(
        principal, null, parseRoles(String.valueOf(claims.get("roles"))));
  }

  private List<SimpleGrantedAuthority> parseRoles(String roles) {
    if (!StringUtils.hasText(roles)) {
      return List.of();
    }
    String cleaned = roles.trim();
    if (cleaned.startsWith("[") && cleaned.endsWith("]") && cleaned.length() >= 2) {
      cleaned = cleaned.substring(1, cleaned.length() - 1);
    }
    return Arrays.stream(cleaned.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(r -> r.replace("\"", "").replace("'", "").trim())
        .filter(StringUtils::hasText)
        .map(
            r -> {
              String u = r.toUpperCase(Locale.ROOT);
              return u.startsWith("ROLE_") ? u : "ROLE_" + u;
            })
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
