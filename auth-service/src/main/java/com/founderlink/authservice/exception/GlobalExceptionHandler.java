package com.founderlink.authservice.exception;

import com.founderlink.authservice.dto.ApiErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.util.Comparator;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps domain and framework exceptions to the standard {@link ApiErrorResponse} envelope. */
@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ApiErrorResponse> handleUnauthorized(
      UnauthorizedException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(
            ApiErrorResponse.of(HttpStatus.UNAUTHORIZED, ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(BadRequestException.class)
  public ResponseEntity<ApiErrorResponse> handleBadRequest(
      BadRequestException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(ConflictException.class)
  public ResponseEntity<ApiErrorResponse> handleConflict(
      ConflictException ex, HttpServletRequest request) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of(HttpStatus.CONFLICT, ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ApiErrorResponse> handleValidation(
      MethodArgumentNotValidException ex, HttpServletRequest request) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(
                HttpStatus.BAD_REQUEST, "Validation failed", request.getRequestURI(), details));
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiErrorResponse> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    return ResponseEntity.badRequest()
        .body(
            ApiErrorResponse.of(HttpStatus.BAD_REQUEST, ex.getMessage(), request.getRequestURI()));
  }

  @ExceptionHandler(DataIntegrityViolationException.class)
  public ResponseEntity<ApiErrorResponse> handleDataIntegrity(
      DataIntegrityViolationException ex, HttpServletRequest request) {
    String message = "Data constraint violation";
    if (ex.getMostSpecificCause() != null && ex.getMostSpecificCause().getMessage() != null) {
      String m = ex.getMostSpecificCause().getMessage();
      if (m.contains("Duplicate") || m.contains("UK_") || m.contains("unique")) {
        message = "A record with this unique key already exists";
      }
    }
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ApiErrorResponse.of(HttpStatus.CONFLICT, message, request.getRequestURI()));
  }
}
