package com.founderlink.userservice.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CoFounderProfileUpsertRequest(
    @NotBlank(message = "name must not be blank") String name,
    @NotBlank(message = "email must not be blank") @Email(message = "email must be valid")
        String email,
    String skills,
    String experience,
    String bio,
    String portfolioLinks,
    @NotBlank(message = "expertise must not be blank") String expertise,
    String coFounderExperience) {}
