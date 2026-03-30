package com.founderlink.startupservice.exception;

public class ForbiddenUpdateException extends RuntimeException {
  public ForbiddenUpdateException(String message) {
    super(message);
  }
}
