package com.founderlink.authservice.exception;

public class ConflictException extends RuntimeException {
  public ConflictException(String message) {
    super(message);
  }
}
