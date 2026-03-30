package com.founderlink.authservice.controller;

import com.founderlink.authservice.dto.AuthResponse;
import com.founderlink.authservice.dto.LoginRequest;
import com.founderlink.authservice.dto.RegisterRequest;
import com.founderlink.authservice.dto.RegisterResponse;
import com.founderlink.authservice.exception.UnauthorizedException;
import com.founderlink.authservice.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication REST API aligned with the FounderLink design: register (no JWT), login (JWT pair),
 * refresh.
 */
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Self-service signup. Returns acknowledgment only — tokens are issued exclusively via {@code
   * /auth/login}.
   */
  @PostMapping("/register")
  @ResponseStatus(HttpStatus.CREATED)
  public RegisterResponse register(@Valid @RequestBody RegisterRequest request) {
    return authService.register(request);
  }

  /** Credential check; returns short-lived access token and refresh token on success. */
  @PostMapping("/login")
  public AuthResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }

  /**
   * Exchanges a valid refresh bearer for a new access token (refresh bearer stays unchanged in the
   * client).
   */
  @PostMapping("/refresh")
  public AuthResponse refresh(
      HttpServletRequest httpServletRequest, Authentication authentication) {
    if (authentication == null || authentication.getName() == null) {
      throw new UnauthorizedException("Missing authentication for refresh");
    }

    Long userId;
    try {
      userId = Long.valueOf(authentication.getName());
    } catch (NumberFormatException ex) {
      throw new UnauthorizedException("Invalid authenticated user id");
    }

    AuthResponse refreshed = authService.refresh(userId);
    String refreshToken = extractBearer(httpServletRequest.getHeader("Authorization"));
    return new AuthResponse(refreshed.accessToken(), refreshToken);
  }

  private String extractBearer(String authorizationHeader) {
    if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
      throw new UnauthorizedException("Missing bearer refresh token");
    }
    return authorizationHeader.substring(7);
  }
}
