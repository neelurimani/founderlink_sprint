package com.founderlink.messagingservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class JwtSupport {

  @Value("${security.jwt.secret:${JWT_SECRET:founderlink-dev-secret-key-change-me-123456}}")
  private String jwtSecret;

  public Claims parseToken(String token) {
    SecretKey key = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    return Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
  }

  public String extractBearerToken(String authorizationHeader) {
    if (!StringUtils.hasText(authorizationHeader) || !authorizationHeader.startsWith("Bearer ")) {
      return null;
    }
    return authorizationHeader.substring(7);
  }
}
