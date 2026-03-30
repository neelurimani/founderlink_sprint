package com.founderlink.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record FounderProfileUpsertRequest(
    @NotBlank(message = "name must not be blank") String name,
    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid")
        String email,
    String skills,
    String experience,
    String bio,
    String portfolioLinks,
    @NotBlank(message = "startupName must not be blank") String startupName,
    @NotBlank(message = "industry must not be blank") String industry,
    @NotNull(message = "fundingGoal must not be null")
        @Positive(message = "fundingGoal must be positive")
        Double fundingGoal) {}
