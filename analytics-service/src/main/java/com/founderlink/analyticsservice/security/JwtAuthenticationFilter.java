package com.founderlink.analyticsservice.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
      throws ServletException, java.io.IOException {
    if (SecurityContextHolder.getContext().getAuthentication() == null) {
      String userId = request.getHeader("X-User-Id");
      String roles = request.getHeader("X-Roles");

      if (StringUtils.hasText(userId)) {
        UsernamePasswordAuthenticationToken authentication =
            new UsernamePasswordAuthenticationToken(userId, null, parseRoles(roles));
        SecurityContextHolder.getContext().setAuthentication(authentication);
      }
    }

    filterChain.doFilter(request, response);
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
