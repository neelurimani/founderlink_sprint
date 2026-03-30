package com.founderlink.apigateway.routing;

import static org.assertj.core.api.Assertions.assertThat;

import com.founderlink.apigateway.config.GatewayProperties;
import java.net.URI;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RouteResolverTest {

  @Test
  void resolvesAndStripsPrefix() {
    GatewayProperties properties = new GatewayProperties();
    GatewayProperties.Route route = new GatewayProperties.Route();
    route.setId("user-service");
    route.setPathPrefix("/api/users");
    route.setStripPathPrefix(true);
    route.setTargetBaseUri(URI.create("http://localhost:8081"));
    properties.setRoutes(List.of(route));

    RouteResolver routeResolver = new RouteResolver(properties);
    ResolvedRoute resolved =
        routeResolver.resolve("/api/users/profile/123", "active=true").orElseThrow();

    assertThat(resolved.route().getId()).isEqualTo("user-service");
    assertThat(resolved.targetUri().toString())
        .isEqualTo("http://localhost:8081/profile/123?active=true");
  }

  @Test
  void preservesPrefixWhenConfigured() {
    GatewayProperties properties = new GatewayProperties();
    GatewayProperties.Route route = new GatewayProperties.Route();
    route.setId("project-service");
    route.setPathPrefix("/api/projects");
    route.setStripPathPrefix(false);
    route.setTargetBaseUri(URI.create("http://localhost:8082"));
    properties.setRoutes(List.of(route));

    RouteResolver routeResolver = new RouteResolver(properties);
    ResolvedRoute resolved = routeResolver.resolve("/api/projects/42", null).orElseThrow();

    assertThat(resolved.targetUri().toString()).isEqualTo("http://localhost:8082/api/projects/42");
  }

  @Test
  void resolvesPrefixRootWithoutTrailingSlash() {
    GatewayProperties properties = new GatewayProperties();
    GatewayProperties.Route route = new GatewayProperties.Route();
    route.setId("user-service");
    route.setPathPrefix("/api/users");
    route.setStripPathPrefix(true);
    route.setTargetBaseUri(URI.create("http://localhost:8081/users"));
    properties.setRoutes(List.of(route));

    RouteResolver routeResolver = new RouteResolver(properties);
    ResolvedRoute resolved = routeResolver.resolve("/api/users", null).orElseThrow();

    assertThat(resolved.targetUri().toString()).isEqualTo("http://localhost:8081/users");
  }
}
