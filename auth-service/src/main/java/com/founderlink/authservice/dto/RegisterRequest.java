package com.founderlink.authservice.dto;

import com.founderlink.authservice.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
    @NotBlank(message = "name must not be blank")
        @Size(max = 100, message = "name must be at most 100 characters")
        @Pattern(
            regexp = ValidationPatterns.PERSON_NAME,
            message = "name must contain only letters and spaces")
        String name,
    @NotBlank(message = "email must not be blank")
        @Size(max = 254, message = "email is too long")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = "email format is invalid")
        String email,
    @NotBlank(message = "password must not be blank")
        @Pattern(
            regexp = ValidationPatterns.PASSWORD,
            message =
                "password must be 8–128 chars with upper, lower, digit, and special character")
        String password,
    @NotBlank(message = "role must not be blank") String role,
    String adminCode) {}
