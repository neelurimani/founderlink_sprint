package com.founderlink.investmentservice.client.fallback;

import feign.FeignException;
import feign.Request;
import feign.Response;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

final class FeignFallbackSupport {

  private FeignFallbackSupport() {}

  /**
   * Preserve upstream {@link FeignException}; otherwise synthesize HTTP 503 so callers map to
   * service-unavailable (not generic HTTP -1 / unknown).
   */
  static FeignException toFeignException(String methodKey, String reason, Throwable cause) {
    if (cause instanceof FeignException fe) {
      return fe;
    }
    Request request =
        Request.create(
            Request.HttpMethod.GET,
            "/",
            Collections.emptyMap(),
            null,
            StandardCharsets.UTF_8,
            null);
    Response response =
        Response.builder().status(503).reason(reason).request(request).build();
    return FeignException.errorStatus(methodKey, response);
  }
}
