package com.founderlink.userservice.exception;

import com.founderlink.userservice.dto.ErrorResponse;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class GlobalExceptionHandler {

  @ExceptionHandler(UserProfileNotFoundException.class)
  public ResponseEntity<ErrorResponse> handleNotFound(UserProfileNotFoundException ex) {
    return ResponseEntity.status(HttpStatus.NOT_FOUND)
        .body(ErrorResponse.withMessage(ex.getMessage()));
  }

  @ExceptionHandler(DuplicateUserProfileException.class)
  public ResponseEntity<ErrorResponse> handleConflict(DuplicateUserProfileException ex) {
    return ResponseEntity.status(HttpStatus.CONFLICT)
        .body(ErrorResponse.withMessage(ex.getMessage()));
  }

  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
    List<String> details =
        ex.getBindingResult().getFieldErrors().stream()
            .sorted(Comparator.comparing(FieldError::getField))
            .map(err -> err.getField() + ": " + err.getDefaultMessage())
            .toList();
    return ResponseEntity.badRequest().body(new ErrorResponse("Validation failed", details));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
    return ResponseEntity.badRequest().body(ErrorResponse.withMessage(ex.getMessage()));
  }

  @ExceptionHandler(UnauthorizedException.class)
  public ResponseEntity<ErrorResponse> handleUnauthorized(UnauthorizedException ex) {
    return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
        .body(ErrorResponse.withMessage(ex.getMessage()));
  }

  @ExceptionHandler(ForbiddenUpdateException.class)
  public ResponseEntity<ErrorResponse> handleForbidden(ForbiddenUpdateException ex) {
    return ResponseEntity.status(HttpStatus.FORBIDDEN)
        .body(ErrorResponse.withMessage(ex.getMessage()));
  }
}
