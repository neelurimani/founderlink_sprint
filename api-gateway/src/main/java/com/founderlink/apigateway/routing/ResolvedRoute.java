package com.founderlink.apigateway.routing;

import com.founderlink.apigateway.config.GatewayProperties;
import java.net.URI;

public record ResolvedRoute(GatewayProperties.Route route, URI targetUri) {}
