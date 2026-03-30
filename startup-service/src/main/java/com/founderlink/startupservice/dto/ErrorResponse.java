package com.founderlink.startupservice.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.time.Instant;
import java.util.List;
import org.springframework.http.HttpStatus;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(
    Instant timestamp,
    int status,
    String error,
    String message,
    String path,
    List<String> details) {
  public static ErrorResponse of(HttpStatus httpStatus, String message, String path) {
    return new ErrorResponse(
        Instant.now(), httpStatus.value(), httpStatus.getReasonPhrase(), message, path, List.of());
  }

  public static ErrorResponse of(
      HttpStatus httpStatus, String message, String path, List<String> details) {
    return new ErrorResponse(
        Instant.now(),
        httpStatus.value(),
        httpStatus.getReasonPhrase(),
        message,
        path,
        details == null ? List.of() : details);
  }
}
