package com.founderlink.authservice.service;

import com.founderlink.authservice.config.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Collection;
import java.util.Date;
import java.util.Locale;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Signs and parses JWT access/refresh tokens using the shared platform secret from Config Server.
 */
@Service
public class JwtService {

  private final SecretKey key;
  private final long accessTokenExpirySeconds;
  private final long refreshTokenExpirySeconds;

  public JwtService(
      JwtProperties jwtProperties,
      @Value("${security.jwt.secret:${JWT_SECRET:founderlink-dev-secret-key-change-me-123456}}")
          String jwtSecret) {
    this.key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    this.accessTokenExpirySeconds = jwtProperties.getAccessTokenExpiryMinutes() * 60;
    this.refreshTokenExpirySeconds = jwtProperties.getRefreshTokenExpiryDays() * 24 * 60 * 60;
  }

  public String generateAccessToken(Long userId, Collection<String> roles) {
    return generateToken(userId, roles, "access", accessTokenExpirySeconds);
  }

  public String generateRefreshToken(Long userId, Collection<String> roles) {
    return generateToken(userId, roles, "refresh", refreshTokenExpirySeconds);
  }

  public Claims parseClaims(String token) {
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  private String generateToken(
      Long userId, Collection<String> roles, String tokenType, long expirySeconds) {
    Instant now = Instant.now();
    Instant exp = now.plusSeconds(expirySeconds);

    String rolesCsv = normalizeRolesCsv(roles);

    return Jwts.builder()
        .subject(String.valueOf(userId))
        .claim("userId", String.valueOf(userId))
        .claim("roles", rolesCsv)
        .claim("tokenType", tokenType)
        .issuedAt(Date.from(now))
        .expiration(Date.from(exp))
        .signWith(key, SignatureAlgorithm.HS256)
        .compact();
  }

  private String normalizeRolesCsv(Collection<String> roles) {
    if (roles == null || roles.isEmpty()) {
      return "";
    }
    return roles.stream()
        .filter(StringUtils::hasText)
        .map(String::trim)
        .map(s -> s.toUpperCase(Locale.ROOT))
        .map(s -> s.startsWith("ROLE_") ? s.substring("ROLE_".length()) : s)
        .reduce((a, b) -> a + "," + b)
        .orElse("");
  }
}
