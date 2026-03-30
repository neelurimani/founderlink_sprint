package com.founderlink.authservice.dto;

/** Registration does not return JWTs — only {@link AuthResponse} is used for login/refresh. */
public record RegisterResponse(String message, String userId, String email) {
  public static RegisterResponse success(Long userId, String email) {
    return new RegisterResponse("User registered successfully", String.valueOf(userId), email);
  }
}
