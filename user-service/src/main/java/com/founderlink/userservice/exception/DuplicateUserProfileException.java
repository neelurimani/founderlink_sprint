package com.founderlink.userservice.exception;

public class DuplicateUserProfileException extends RuntimeException {

  public DuplicateUserProfileException(String message) {
    super(message);
  }
}
