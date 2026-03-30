package com.founderlink.apigateway.idempotency;

import java.util.Arrays;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

@Component
public class IdempotencyStore {

  private final Map<String, CompletedRequest> completedRequests = new ConcurrentHashMap<>();
  private final Map<String, InFlightRequest> inFlightRequests = new ConcurrentHashMap<>();

  public LookupResult lookupCompleted(String key, String fingerprint) {
    CompletedRequest completed = completedRequests.get(key);
    if (completed == null) {
      return LookupResult.noneResult();
    }
    if (!completed.fingerprint().equals(fingerprint)) {
      return LookupResult.conflictResult();
    }
    return LookupResult.replayResult(completed.asResponseEntity());
  }

  public Mono<ResponseEntity<byte[]>> runOrJoin(
      String key,
      String fingerprint,
      java.util.function.Supplier<Mono<ResponseEntity<byte[]>>> supplier) {
    InFlightRequest existing = inFlightRequests.get(key);
    if (existing != null) {
      if (!existing.fingerprint().equals(fingerprint)) {
        return Mono.just(
            ResponseEntity.status(409)
                .body("Idempotency-Key reused with different payload".getBytes()));
      }
      return existing.responseMono();
    }

    Mono<ResponseEntity<byte[]>> builtMono =
        supplier
            .get()
            .map(
                response -> {
                  completedRequests.put(key, CompletedRequest.from(fingerprint, response));
                  return response;
                })
            .doFinally(signal -> inFlightRequests.remove(key))
            .cache();

    InFlightRequest created = new InFlightRequest(fingerprint, builtMono);
    InFlightRequest race = inFlightRequests.putIfAbsent(key, created);
    if (race != null) {
      if (!race.fingerprint().equals(fingerprint)) {
        return Mono.just(
            ResponseEntity.status(409)
                .body("Idempotency-Key reused with different payload".getBytes()));
      }
      return race.responseMono();
    }

    return builtMono;
  }

  public record LookupResult(
      boolean replay, boolean conflict, ResponseEntity<byte[]> cachedResponse) {
    static LookupResult noneResult() {
      return new LookupResult(false, false, null);
    }

    static LookupResult conflictResult() {
      return new LookupResult(false, true, null);
    }

    static LookupResult replayResult(ResponseEntity<byte[]> cachedResponse) {
      return new LookupResult(true, false, cachedResponse);
    }
  }

  private record InFlightRequest(String fingerprint, Mono<ResponseEntity<byte[]>> responseMono) {}

  private record CompletedRequest(
      String fingerprint, int status, HttpHeaders headers, byte[] body) {
    static CompletedRequest from(String fingerprint, ResponseEntity<byte[]> response) {
      HttpHeaders copiedHeaders = new HttpHeaders();
      copiedHeaders.putAll(response.getHeaders());
      byte[] responseBody =
          response.getBody() == null
              ? new byte[0]
              : Arrays.copyOf(response.getBody(), response.getBody().length);
      return new CompletedRequest(
          fingerprint, response.getStatusCode().value(), copiedHeaders, responseBody);
    }

    ResponseEntity<byte[]> asResponseEntity() {
      HttpHeaders copiedHeaders = new HttpHeaders();
      copiedHeaders.putAll(headers);
      return ResponseEntity.status(HttpStatusCode.valueOf(status))
          .headers(copiedHeaders)
          .body(Arrays.copyOf(body, body.length));
    }
  }
}
