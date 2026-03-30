package com.founderlink.authservice.service;

import com.founderlink.authservice.dto.AuthResponse;
import com.founderlink.authservice.dto.LoginRequest;
import com.founderlink.authservice.dto.RegisterRequest;
import com.founderlink.authservice.dto.RegisterResponse;
import com.founderlink.authservice.entity.AppUser;
import com.founderlink.authservice.entity.RoleEntity;
import com.founderlink.authservice.exception.BadRequestException;
import com.founderlink.authservice.exception.ConflictException;
import com.founderlink.authservice.exception.UnauthorizedException;
import com.founderlink.authservice.repo.RoleRepository;
import com.founderlink.authservice.repo.UserRepository;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Core authentication flows: registration (no token issuance), login, and refresh token exchange.
 */
@Service
@RequiredArgsConstructor
public class AuthService {

  private static final Set<String> SUPPORTED_ROLES =
      Set.of("FOUNDER", "INVESTOR", "COFOUNDER", "ADMIN");

  private final UserRepository userRepository;
  private final RoleRepository roleRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  @Value("${founderlink.admin-registration.enabled:true}")
  private boolean adminRegistrationEnabled;

  @Value("${founderlink.admin-registration.code:${ADMIN_REGISTRATION_CODE:}}")
  private String adminRegistrationCode;

  /**
   * Persists the user and roles. Per security policy, JWTs are not returned — client must call
   * {@code /auth/login}.
   */
  public RegisterResponse register(RegisterRequest request) {
    String email = request.email().trim().toLowerCase();
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ConflictException("A user with this email already exists");
    }

    String role = normalizeRole(request.role());
    validateAdminRegistration(role, request.adminCode());
    RoleEntity roleEntity =
        roleRepository
            .findByNameIgnoreCase(role)
            .orElseGet(() -> roleRepository.save(new RoleEntity(role)));

    AppUser user = new AppUser();
    user.setName(StringUtils.trimAllWhitespace(request.name()));
    user.setEmail(email);
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.getRoles().add(roleEntity);

    AppUser saved = userRepository.save(user);
    return RegisterResponse.success(saved.getId(), saved.getEmail());
  }

  public AuthResponse login(LoginRequest request) {
    String email = request.email().trim().toLowerCase();
    AppUser user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(() -> new UnauthorizedException("Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new UnauthorizedException("Invalid credentials");
    }

    List<String> roles =
        user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList());

    String accessToken = jwtService.generateAccessToken(user.getId(), roles);
    String refreshToken = jwtService.generateRefreshToken(user.getId(), roles);

    return new AuthResponse(accessToken, refreshToken);
  }

  public AuthResponse refresh(Long userId) {
    AppUser user =
        userRepository
            .findById(userId)
            .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));

    List<String> roles =
        user.getRoles().stream().map(RoleEntity::getName).collect(Collectors.toList());

    String accessToken = jwtService.generateAccessToken(userId, roles);
    return new AuthResponse(accessToken, null);
  }

  private String normalizeRole(String role) {
    if (role == null) {
      throw new BadRequestException("role must not be null");
    }
    String normalized = role.trim().toUpperCase();
    if (normalized.startsWith("ROLE_")) {
      normalized = normalized.substring("ROLE_".length());
    }
    if (!SUPPORTED_ROLES.contains(normalized)) {
      throw new BadRequestException("Unsupported role " + role);
    }
    return normalized;
  }

  private void validateAdminRegistration(String normalizedRole, String suppliedAdminCode) {
    if (!"ADMIN".equals(normalizedRole)) {
      return;
    }
    if (!adminRegistrationEnabled) {
      throw new UnauthorizedException("Admin registration is disabled");
    }
    if (!StringUtils.hasText(adminRegistrationCode)) {
      throw new UnauthorizedException("Admin registration code is not configured");
    }
    if (!adminRegistrationCode.equals(suppliedAdminCode)) {
      throw new UnauthorizedException("Invalid admin registration code");
    }
  }
}
