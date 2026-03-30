package com.founderlink.authservice.security;

import com.founderlink.authservice.service.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;

  public JwtAuthenticationFilter(JwtService jwtService) {
    this.jwtService = jwtService;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, IOException {
    // Only protect endpoints (register/login are permitted in SecurityConfig)
    String path = request.getRequestURI();
    boolean isPublic = path.equals("/auth/register") || path.equals("/auth/login");
    if (isPublic) {
      filterChain.doFilter(request, response);
      return;
    }

    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String token = extractBearerToken(request);
      if (StringUtils.hasText(token)) {
        try {
          Claims claims = jwtService.parseClaims(token);
          String tokenType = String.valueOf(claims.get("tokenType"));
          String requiredType = path.equals("/auth/refresh") ? "refresh" : "access";
          if (!requiredType.equals(tokenType)) {
            filterChain.doFilter(request, response);
            return;
          }

          String userId = claims.get("userId", String.class);
          if (!StringUtils.hasText(userId)) {
            userId = claims.getSubject();
          }
          String rolesCsv = String.valueOf(claims.get("roles"));
          UsernamePasswordAuthenticationToken authentication =
              new UsernamePasswordAuthenticationToken(userId, null, parseRoles(rolesCsv));

          SecurityContextHolder.getContext().setAuthentication(authentication);
        } catch (Exception ignored) {
          // Let SecurityConfig return 401.
        }
      }
    }

    filterChain.doFilter(request, response);
  }

  private String extractBearerToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");
    if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
      return null;
    }
    return authHeader.substring(7);
  }

  private List<SimpleGrantedAuthority> parseRoles(String rolesCsv) {
    if (!StringUtils.hasText(rolesCsv)) {
      return List.of();
    }
    return Arrays.stream(rolesCsv.split(","))
        .map(String::trim)
        .filter(StringUtils::hasText)
        .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
        .map(SimpleGrantedAuthority::new)
        .collect(Collectors.toList());
  }
}
