package com.founderlink.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserProfileUpsertRequest(
    @NotBlank(message = "name must not be blank") String name,
    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid")
        String email,
    @NotBlank(message = "role must not be blank") String role,
    String skills,
    String experience,
    String bio,
    String portfolioLinks,
    String startupName,
    String industry,
    Double fundingGoal,
    String expertise,
    String coFounderExperience,
    Double investmentBudget,
    String preferredIndustries) {}
