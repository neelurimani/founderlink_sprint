package com.founderlink.apigateway.routing;

import com.founderlink.apigateway.config.GatewayProperties;
import java.net.URI;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;

@Component
public class RouteResolver {

  private final GatewayProperties gatewayProperties;

  public RouteResolver(GatewayProperties gatewayProperties) {
    this.gatewayProperties = gatewayProperties;
  }

  public Optional<ResolvedRoute> resolve(String requestPath, String rawQuery) {
    for (GatewayProperties.Route route : gatewayProperties.getRoutes()) {
      if (!matchesPrefix(requestPath, route.getPathPrefix())) {
        continue;
      }

      String targetPath = requestPath;
      if (route.isStripPathPrefix()) {
        targetPath = stripPrefix(requestPath, route.getPathPrefix());
      }

      URI targetUri =
          UriComponentsBuilder.fromUri(route.getTargetBaseUri())
              .replacePath(normalizePath(route.getTargetBaseUri().getPath(), targetPath))
              .replaceQuery(rawQuery)
              .build(true)
              .toUri();

      return Optional.of(new ResolvedRoute(route, targetUri));
    }
    return Optional.empty();
  }

  private boolean matchesPrefix(String requestPath, String configuredPrefix) {
    String normalizedPrefix = normalizePrefix(configuredPrefix);
    return requestPath.equals(normalizedPrefix) || requestPath.startsWith(normalizedPrefix + "/");
  }

  private String stripPrefix(String requestPath, String configuredPrefix) {
    String normalizedPrefix = normalizePrefix(configuredPrefix);
    String stripped = requestPath.substring(normalizedPrefix.length());
    if (!StringUtils.hasText(stripped)) {
      return "";
    }
    return stripped.startsWith("/") ? stripped : "/" + stripped;
  }

  private String normalizePrefix(String prefix) {
    if (!StringUtils.hasText(prefix)) {
      return "/";
    }
    String normalized = prefix.startsWith("/") ? prefix : "/" + prefix;
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String normalizePath(String basePath, String routedPath) {
    String left = StringUtils.hasText(basePath) ? basePath : "";
    String right = StringUtils.hasText(routedPath) ? routedPath : "";
    if (!left.startsWith("/")) {
      left = "/" + left;
    }
    if (left.endsWith("/")) {
      left = left.substring(0, left.length() - 1);
    }
    if (!StringUtils.hasText(right)) {
      return left;
    }
    if (!right.startsWith("/")) {
      right = "/" + right;
    }
    return left + right;
  }
}
