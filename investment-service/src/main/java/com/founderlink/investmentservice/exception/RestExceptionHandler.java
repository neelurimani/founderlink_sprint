package com.founderlink.investmentservice.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

/**
 * JSON errors aligned with other FounderLink services (timestamp, status, error, message, path).
 */
@RestControllerAdvice
public class RestExceptionHandler {

  @ExceptionHandler(ResponseStatusException.class)
  public ResponseEntity<ApiErrorResponse> handleResponseStatus(
      ResponseStatusException ex, HttpServletRequest request) {
    HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
    if (status == null) {
      status = HttpStatus.INTERNAL_SERVER_ERROR;
    }
    String message = ex.getReason() != null ? ex.getReason() : status.getReasonPhrase();
    return ResponseEntity.status(status)
        .body(ApiErrorResponse.of(status, message, request.getRequestURI()));
  }
}
