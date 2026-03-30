package com.founderlink.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record InvestorProfileUpsertRequest(
    @NotBlank(message = "name must not be blank") String name,
    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid")
        String email,
    String skills,
    String experience,
    String bio,
    String portfolioLinks,
    @NotNull(message = "investmentBudget must not be null")
        @Positive(message = "investmentBudget must be positive")
        Double investmentBudget,
    @NotBlank(message = "preferredIndustries must not be blank") String preferredIndustries) {}
