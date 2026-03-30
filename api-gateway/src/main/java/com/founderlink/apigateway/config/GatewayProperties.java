package com.founderlink.apigateway.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "gateway")
public class GatewayProperties {

  private final Security security = new Security();
  private final RateLimit rateLimit = new RateLimit();
  @NotEmpty private List<Route> routes = new ArrayList<>();

  /**
   * POST requests whose path starts with one of these prefixes do not require {@code
   * Idempotency-Key} (login/register must work like normal REST clients / Postman).
   */
  private List<String> idempotencyExcludedPathPrefixes = new ArrayList<>(List.of("/api/auth"));

  public List<String> getIdempotencyExcludedPathPrefixes() {
    return idempotencyExcludedPathPrefixes;
  }

  public void setIdempotencyExcludedPathPrefixes(List<String> idempotencyExcludedPathPrefixes) {
    this.idempotencyExcludedPathPrefixes = idempotencyExcludedPathPrefixes;
  }

  public Security getSecurity() {
    return security;
  }

  public RateLimit getRateLimit() {
    return rateLimit;
  }

  public List<Route> getRoutes() {
    return routes;
  }

  public void setRoutes(List<Route> routes) {
    this.routes = routes;
  }

  public static class Security {
    @NotBlank private String apiKeyHeader = "X-API-Key";
    private List<String> validApiKeys = new ArrayList<>();
    private List<String> unsecuredPaths = List.of("/actuator/**");
    private boolean jwtEnabled = true;
    private boolean apiKeyEnabled = false;
    @NotBlank private String jwtSecret = "founderlink-dev-secret-key-change-me-123456";
    private String authHeader = "Authorization";

    public String getApiKeyHeader() {
      return apiKeyHeader;
    }

    public void setApiKeyHeader(String apiKeyHeader) {
      this.apiKeyHeader = apiKeyHeader;
    }

    public List<String> getValidApiKeys() {
      return validApiKeys;
    }

    public void setValidApiKeys(List<String> validApiKeys) {
      this.validApiKeys = validApiKeys;
    }

    public List<String> getUnsecuredPaths() {
      return unsecuredPaths;
    }

    public void setUnsecuredPaths(List<String> unsecuredPaths) {
      this.unsecuredPaths = unsecuredPaths;
    }

    public boolean isJwtEnabled() {
      return jwtEnabled;
    }

    public void setJwtEnabled(boolean jwtEnabled) {
      this.jwtEnabled = jwtEnabled;
    }

    public boolean isApiKeyEnabled() {
      return apiKeyEnabled;
    }

    public void setApiKeyEnabled(boolean apiKeyEnabled) {
      this.apiKeyEnabled = apiKeyEnabled;
    }

    public String getJwtSecret() {
      return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
      this.jwtSecret = jwtSecret;
    }

    public String getAuthHeader() {
      return authHeader;
    }

    public void setAuthHeader(String authHeader) {
      this.authHeader = authHeader;
    }
  }

  public static class RateLimit {
    @Min(1)
    private int requestsPerMinute = 120;

    public int getRequestsPerMinute() {
      return requestsPerMinute;
    }

    public void setRequestsPerMinute(int requestsPerMinute) {
      this.requestsPerMinute = requestsPerMinute;
    }
  }

  public static class Route {
    @NotBlank private String id;
    @NotBlank private String pathPrefix;
    private boolean stripPathPrefix = true;
    private URI targetBaseUri;

    public String getId() {
      return id;
    }

    public void setId(String id) {
      this.id = id;
    }

    public String getPathPrefix() {
      return pathPrefix;
    }

    public void setPathPrefix(String pathPrefix) {
      this.pathPrefix = pathPrefix;
    }

    public boolean isStripPathPrefix() {
      return stripPathPrefix;
    }

    public void setStripPathPrefix(boolean stripPathPrefix) {
      this.stripPathPrefix = stripPathPrefix;
    }

    public URI getTargetBaseUri() {
      return targetBaseUri;
    }

    public void setTargetBaseUri(URI targetBaseUri) {
      this.targetBaseUri = targetBaseUri;
    }
  }
}
