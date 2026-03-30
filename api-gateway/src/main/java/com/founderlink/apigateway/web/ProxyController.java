package com.founderlink.apigateway.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.founderlink.apigateway.config.GatewayProperties;
import com.founderlink.apigateway.idempotency.IdempotencyStore;
import com.founderlink.apigateway.routing.ResolvedRoute;
import com.founderlink.apigateway.routing.RouteResolver;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RestController
public class ProxyController {

  private static final Logger log = LoggerFactory.getLogger(ProxyController.class);

  private static final String ROUTE_ID_HEADER = "X-Gateway-Route-Id";
  private static final String IDEMPOTENCY_KEY_HEADER = "Idempotency-Key";
  private static final String PATH_HEADER = "X-Forwarded-Path";
  private static final String HOST_HEADER = HttpHeaders.HOST;
  private static final String CONTENT_LENGTH_HEADER = HttpHeaders.CONTENT_LENGTH;

  private final RouteResolver routeResolver;
  private final GatewayProperties gatewayProperties;
  private final IdempotencyStore idempotencyStore;
  private final WebClient webClient;
  private final ObjectMapper objectMapper;

  public ProxyController(
      RouteResolver routeResolver,
      GatewayProperties gatewayProperties,
      IdempotencyStore idempotencyStore,
      WebClient.Builder webClientBuilder,
      ObjectMapper objectMapper) {
    this.routeResolver = routeResolver;
    this.gatewayProperties = gatewayProperties;
    this.idempotencyStore = idempotencyStore;
    this.webClient = webClientBuilder.build();
    this.objectMapper = objectMapper;
  }

  @RequestMapping("/api/**")
  public Mono<ResponseEntity<byte[]>> proxy(
      ServerHttpRequest request, @RequestBody(required = false) Mono<byte[]> body) {
    HttpMethod method = request.getMethod();
    if (method == null) {
      return badRequest(request, "Unsupported HTTP method");
    }

    String requestPath = request.getURI().getPath();
    String rawQuery = request.getURI().getRawQuery();
    log.info("Gateway client request {} {}", method, requestPath);

    return routeResolver
        .resolve(requestPath, rawQuery)
        .map(resolvedRoute -> forwardRequest(request, method, body, resolvedRoute))
        .orElseGet(() -> notFound(request));
  }

  private Mono<ResponseEntity<byte[]>> forwardRequest(
      ServerHttpRequest request,
      HttpMethod method,
      Mono<byte[]> body,
      ResolvedRoute resolvedRoute) {
    Mono<byte[]> payload =
        requiresRequestBody(method) ? body.defaultIfEmpty(new byte[0]) : Mono.just(new byte[0]);

    return payload
        .flatMap(
            data ->
                withIdempotency(
                    request,
                    method,
                    resolvedRoute,
                    data,
                    () -> forwardDownstream(request, method, resolvedRoute, data)))
        .onErrorResume(
            ex -> {
              log.error(
                  "Gateway proxy failed method={} path={} routeId={} targetUri={}: {}",
                  method,
                  request.getURI().getPath(),
                  resolvedRoute.route().getId(),
                  resolvedRoute.targetUri(),
                  ex.toString(),
                  ex);
              return jsonError(
                  HttpStatus.BAD_GATEWAY,
                  HttpStatus.BAD_GATEWAY.getReasonPhrase(),
                  "Bad gateway: " + ex.getMessage(),
                  request.getURI().getPath());
            });
  }

  private Mono<ResponseEntity<byte[]>> withIdempotency(
      ServerHttpRequest request,
      HttpMethod method,
      ResolvedRoute resolvedRoute,
      byte[] data,
      java.util.function.Supplier<Mono<ResponseEntity<byte[]>>> forwardSupplier) {
    if (method != HttpMethod.POST) {
      return forwardSupplier.get();
    }

    if (isIdempotencyExempt(request.getURI().getPath())) {
      return forwardSupplier.get();
    }

    String idempotencyKey = request.getHeaders().getFirst(IDEMPOTENCY_KEY_HEADER);
    if (idempotencyKey == null || idempotencyKey.isBlank()) {
      return jsonError(
          HttpStatus.BAD_REQUEST,
          HttpStatus.BAD_REQUEST.getReasonPhrase(),
          "Missing Idempotency-Key header for POST request",
          request.getURI().getPath());
    }

    String compositeKey = resolvedRoute.route().getId() + ":" + idempotencyKey.trim();
    String fingerprint = buildRequestFingerprint(method, resolvedRoute, data);
    IdempotencyStore.LookupResult lookup =
        idempotencyStore.lookupCompleted(compositeKey, fingerprint);
    if (lookup.conflict()) {
      return jsonError(
          HttpStatus.CONFLICT,
          HttpStatus.CONFLICT.getReasonPhrase(),
          "Idempotency-Key reused with different payload",
          request.getURI().getPath());
    }
    if (lookup.replay()) {
      return Mono.just(lookup.cachedResponse());
    }

    return idempotencyStore.runOrJoin(compositeKey, fingerprint, forwardSupplier);
  }

  private Mono<ResponseEntity<byte[]>> forwardDownstream(
      ServerHttpRequest request, HttpMethod method, ResolvedRoute resolvedRoute, byte[] data) {
    log.info(
        "Gateway forwarding method={} routeId={} targetUri={}",
        method,
        resolvedRoute.route().getId(),
        resolvedRoute.targetUri());

    var spec =
        webClient
            .method(method)
            .uri(resolvedRoute.targetUri())
            .headers(headers -> copyRequestHeaders(request, headers));

    if (requiresRequestBody(method)) {
      return spec.bodyValue(data)
          .exchangeToMono(resp -> mapDownstreamResponse(resolvedRoute, resp));
    }
    return spec.exchangeToMono(resp -> mapDownstreamResponse(resolvedRoute, resp));
  }

  private Mono<ResponseEntity<byte[]>> mapDownstreamResponse(
      ResolvedRoute resolvedRoute, ClientResponse response) {
    return response
        .bodyToMono(byte[].class)
        .defaultIfEmpty(new byte[0])
        .map(
            bytes -> {
              HttpHeaders outgoingHeaders = new HttpHeaders();
              outgoingHeaders.addAll(response.headers().asHttpHeaders());
              outgoingHeaders.remove(TRANSFER_ENCODING);
              outgoingHeaders.set(ROUTE_ID_HEADER, resolvedRoute.route().getId());
              return ResponseEntity.status(response.statusCode())
                  .headers(outgoingHeaders)
                  .body(bytes);
            });
  }

  private String buildRequestFingerprint(
      HttpMethod method, ResolvedRoute resolvedRoute, byte[] data) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      digest.update(method.name().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) ':');
      digest.update(resolvedRoute.targetUri().toString().getBytes(StandardCharsets.UTF_8));
      digest.update((byte) ':');
      digest.update(data);
      byte[] hashed = digest.digest();
      StringBuilder builder = new StringBuilder();
      for (byte b : hashed) {
        builder.append(String.format("%02x", b));
      }
      return builder.toString();
    } catch (Exception ex) {
      return method.name() + ":" + resolvedRoute.targetUri() + ":" + data.length;
    }
  }

  private boolean isIdempotencyExempt(String path) {
    if (path == null || path.isBlank()) {
      return false;
    }
    for (String prefix : gatewayProperties.getIdempotencyExcludedPathPrefixes()) {
      if (prefix != null && !prefix.isBlank() && path.startsWith(prefix)) {
        return true;
      }
    }
    return false;
  }

  private Mono<ResponseEntity<byte[]>> jsonError(
      HttpStatus status, String error, String message, String path) {
    try {
      var root = objectMapper.createObjectNode();
      root.put("timestamp", Instant.now().toString());
      root.put("status", status.value());
      root.put("error", error);
      root.put("message", message);
      root.put("path", path != null ? path : "");
      byte[] bytes = objectMapper.writeValueAsBytes(root);
      return Mono.just(
          ResponseEntity.status(status).contentType(MediaType.APPLICATION_JSON).body(bytes));
    } catch (Exception ex) {
      log.error("Failed to serialize gateway error JSON: {}", ex.toString());
      return Mono.just(
          ResponseEntity.status(status).body(message.getBytes(StandardCharsets.UTF_8)));
    }
  }

  private void copyRequestHeaders(ServerHttpRequest request, HttpHeaders outbound) {
    HttpHeaders inbound = request.getHeaders();
    outbound.clear();
    outbound.addAll(inbound);
    outbound.remove(HOST_HEADER);
    outbound.remove(CONTENT_LENGTH_HEADER);
    outbound.set(PATH_HEADER, request.getURI().getRawPath());
  }

  /**
   * True for methods that may carry a request body. GET/HEAD must not use {@code bodyValue} on
   * WebClient (some downstream stacks reject it and surfaced as 502).
   */
  private boolean requiresRequestBody(HttpMethod method) {
    return method == HttpMethod.POST
        || method == HttpMethod.PUT
        || method == HttpMethod.PATCH
        || method == HttpMethod.DELETE;
  }

  private Mono<ResponseEntity<byte[]>> notFound(ServerHttpRequest request) {
    String path = request.getURI().getPath();
    return jsonError(
        HttpStatus.NOT_FOUND,
        HttpStatus.NOT_FOUND.getReasonPhrase(),
        "No route configured for path: " + path,
        path);
  }

  private Mono<ResponseEntity<byte[]>> badRequest(ServerHttpRequest request, String message) {
    return jsonError(
        HttpStatus.BAD_REQUEST,
        HttpStatus.BAD_REQUEST.getReasonPhrase(),
        message,
        request.getURI().getPath());
  }

  private static final String TRANSFER_ENCODING = HttpHeaders.TRANSFER_ENCODING;

  @RequestMapping("/gateway/routes")
  public Map<String, Object> simpleRoutes() {
    Map<String, Object> response = new LinkedHashMap<>();
    response.put("status", "ok");
    List<Map<String, String>> routes = new ArrayList<>();
    for (GatewayProperties.Route route : gatewayProperties.getRoutes()) {
      Map<String, String> item = new LinkedHashMap<>();
      item.put("id", route.getId());
      item.put("pathPrefix", route.getPathPrefix());
      item.put("targetBaseUri", String.valueOf(route.getTargetBaseUri()));
      routes.add(item);
    }
    response.put("routes", routes);
    return response;
  }

  @RequestMapping("/system/status")
  public Mono<Map<String, Object>> systemStatus() {
    List<GatewayProperties.Route> coreRoutes =
        gatewayProperties.getRoutes().stream()
            .filter(route -> !route.getId().endsWith("-docs"))
            .sorted(Comparator.comparing(GatewayProperties.Route::getId))
            .toList();

    Set<String> visitedHealthUris =
        coreRoutes.stream()
            .map(route -> toHealthUri(route.getTargetBaseUri()).toString())
            .collect(Collectors.toSet());

    return Flux.fromIterable(coreRoutes)
        .concatMap(
            route -> checkServiceHealth(route.getId(), toHealthUri(route.getTargetBaseUri())))
        .collectList()
        .map(
            services -> {
              Map<String, Object> response = new LinkedHashMap<>();
              boolean allUp =
                  services.stream().allMatch(service -> "UP".equals(service.get("status")));
              response.put("status", allUp ? "UP" : "DEGRADED");
              response.put("servicesChecked", services.size());
              response.put("distinctHealthUris", visitedHealthUris.size());
              response.put("services", services);
              return response;
            });
  }

  private Mono<Map<String, Object>> checkServiceHealth(String serviceId, URI healthUri) {
    return webClient
        .get()
        .uri(healthUri)
        .exchangeToMono(
            response -> Mono.just(toServiceStatus(serviceId, healthUri, response.statusCode())))
        .onErrorResume(ex -> Mono.just(toServiceStatus(serviceId, healthUri, null)));
  }

  private Map<String, Object> toServiceStatus(
      String serviceId, URI healthUri, HttpStatusCode code) {
    Map<String, Object> service = new LinkedHashMap<>();
    service.put("serviceId", serviceId);
    service.put("healthUrl", healthUri.toString());
    boolean up = code != null && code.is2xxSuccessful();
    service.put("status", up ? "UP" : "DOWN");
    service.put("httpStatus", code == null ? "N/A" : code.value());
    return service;
  }

  private URI toHealthUri(URI targetBaseUri) {
    int port = targetBaseUri.getPort();
    return URI.create(
        targetBaseUri.getScheme()
            + "://"
            + targetBaseUri.getHost()
            + (port > 0 ? ":" + port : "")
            + "/actuator/health");
  }
}
