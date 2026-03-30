package com.founderlink.userservice.exception;

public class ForbiddenUpdateException extends RuntimeException {
  public ForbiddenUpdateException(String message) {
    super(message);
  }
}
