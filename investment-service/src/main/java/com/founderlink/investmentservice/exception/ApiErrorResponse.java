package com.founderlink.investmentservice.exception;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details) {
  public static ApiErrorResponse of(HttpStatus httpStatus, String message, String path) {
    return new ApiErrorResponse(
        Instant.now(), httpStatus.value(), httpStatus.getReasonPhrase(), message, path, List.of());
  }
}
