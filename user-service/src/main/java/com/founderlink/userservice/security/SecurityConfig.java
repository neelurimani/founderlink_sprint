package com.founderlink.userservice.security;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception {
    http.csrf(csrf -> csrf.disable())
        .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/v3/api-docs",
                        "/v3/api-docs/**",
                        "/api-docs",
                        "/api-docs/**",
                        "/swagger-ui",
                        "/swagger-ui/**",
                        "/swagger-ui.html",
                        "/actuator/**",
                        "/h2-console/**")
                    .permitAll()
                    // Keep service-level guards for dev integration flows.
                    .anyRequest()
                    .permitAll())
        .exceptionHandling(
            ex ->
                ex.authenticationEntryPoint(
                        (request, response, authException) ->
                            writeJsonError(
                                response, HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized"))
                    .accessDeniedHandler(
                        (request, response, accessDeniedException) ->
                            writeJsonError(
                                response, HttpServletResponse.SC_FORBIDDEN, "Forbidden")))
        .addFilterBefore(
            jwtAuthenticationFilter,
            org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
                .class);

    return http.build();
  }

  private void writeJsonError(HttpServletResponse response, int status, String message)
      throws IOException {
    response.setStatus(status);
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");
    response.getWriter().write("{\"message\":\"" + message + "\"}");
  }
}
