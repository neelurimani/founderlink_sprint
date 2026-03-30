package com.founderlink.startupservice.exception;

/** Business rule conflict (e.g. duplicate startup for same founder). */
public class ConflictException extends RuntimeException {

  public ConflictException(String message) {
    super(message);
  }
}
