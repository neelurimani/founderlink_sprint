package com.founderlink.userservice.controller;

import com.founderlink.userservice.dto.CoFounderProfileUpsertRequest;
import com.founderlink.userservice.dto.FounderProfileUpsertRequest;
import com.founderlink.userservice.dto.InvestorProfileUpsertRequest;
import com.founderlink.userservice.dto.PaginatedResponse;
import com.founderlink.userservice.dto.UserProfileResponse;
import com.founderlink.userservice.dto.UserProfileUpsertRequest;
import com.founderlink.userservice.service.UserProfileUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.net.URI;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP adapters for user profiles. Delegates all rules to {@link UserProfileUseCase} (clean
 * architecture). Base paths align with the design document; role-specific create/update URLs extend
 * the domain model.
 */
@RestController
@RequestMapping("/users")
@Validated
@Tag(name = "Users", description = "User profile management APIs")
public class UserProfileController {

  private final UserProfileUseCase userProfileUseCase;

  public UserProfileController(UserProfileUseCase userProfileUseCase) {
    this.userProfileUseCase = userProfileUseCase;
  }

  @PostMapping
  @Operation(summary = "Create a user profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> create(
      @Valid @RequestBody UserProfileUpsertRequest request) {
    UserProfileResponse response = userProfileUseCase.create(request);
    return ResponseEntity.created(URI.create("/users/" + response.getId())).body(response);
  }

  @PostMapping("/founders")
  @Operation(summary = "Create a founder profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> createFounder(
      @Valid @RequestBody FounderProfileUpsertRequest request) {
    UserProfileResponse response = userProfileUseCase.createFounder(request);
    return ResponseEntity.created(URI.create("/users/" + response.getId())).body(response);
  }

  @PostMapping("/cofounders")
  @Operation(summary = "Create a cofounder profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> createCoFounder(
      @Valid @RequestBody CoFounderProfileUpsertRequest request) {
    UserProfileResponse response = userProfileUseCase.createCoFounder(request);
    return ResponseEntity.created(URI.create("/users/" + response.getId())).body(response);
  }

  @PostMapping("/investors")
  @Operation(summary = "Create an investor profile")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> createInvestor(
      @Valid @RequestBody InvestorProfileUpsertRequest request) {
    UserProfileResponse response = userProfileUseCase.createInvestor(request);
    return ResponseEntity.created(URI.create("/users/" + response.getId())).body(response);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get user profile by user id")
  @PreAuthorize("isAuthenticated()")
  public ResponseEntity<UserProfileResponse> get(@PathVariable("id") Long id) {
    return ResponseEntity.ok(userProfileUseCase.getByUserId(id));
  }

  @GetMapping
  @Operation(summary = "List user profiles with pagination")
  @PreAuthorize("hasRole('ADMIN')")
  public ResponseEntity<PaginatedResponse<UserProfileResponse>> list(
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(PaginatedResponse.from(userProfileUseCase.list(page, size)));
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update user profile by user id")
  @PreAuthorize("isAuthenticated()")
  public UserProfileResponse update(
      @PathVariable("id") Long id, @Valid @RequestBody UserProfileUpsertRequest request) {
    return userProfileUseCase.update(id, request);
  }

  @PutMapping("/founders/{id}")
  @Operation(summary = "Update founder profile by user id")
  @PreAuthorize("isAuthenticated()")
  public UserProfileResponse updateFounder(
      @PathVariable("id") Long id, @Valid @RequestBody FounderProfileUpsertRequest request) {
    return userProfileUseCase.updateFounder(id, request);
  }

  @PutMapping("/cofounders/{id}")
  @Operation(summary = "Update cofounder profile by user id")
  @PreAuthorize("isAuthenticated()")
  public UserProfileResponse updateCoFounder(
      @PathVariable("id") Long id, @Valid @RequestBody CoFounderProfileUpsertRequest request) {
    return userProfileUseCase.updateCoFounder(id, request);
  }

  @PutMapping("/investors/{id}")
  @Operation(summary = "Update investor profile by user id")
  @PreAuthorize("isAuthenticated()")
  public UserProfileResponse updateInvestor(
      @PathVariable("id") Long id, @Valid @RequestBody InvestorProfileUpsertRequest request) {
    return userProfileUseCase.updateInvestor(id, request);
  }
}
