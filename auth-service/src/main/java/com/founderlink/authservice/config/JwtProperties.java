package com.founderlink.authservice.config;

import jakarta.validation.constraints.Positive;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * JWT TTL from Config Server. Signing secret is {@code security.jwt.secret} (shared across
 * services).
 */
@Validated
@ConfigurationProperties(prefix = "founderlink.jwt")
public class JwtProperties {

  @Positive private long accessTokenExpiryMinutes = 60;

  @Positive private long refreshTokenExpiryDays = 7;

  public long getAccessTokenExpiryMinutes() {
    return accessTokenExpiryMinutes;
  }

  public void setAccessTokenExpiryMinutes(long accessTokenExpiryMinutes) {
    this.accessTokenExpiryMinutes = accessTokenExpiryMinutes;
  }

  public long getRefreshTokenExpiryDays() {
    return refreshTokenExpiryDays;
  }

  public void setRefreshTokenExpiryDays(long refreshTokenExpiryDays) {
    this.refreshTokenExpiryDays = refreshTokenExpiryDays;
  }
}
