package com.founderlink.authservice.dto;

import com.founderlink.authservice.validation.ValidationPatterns;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record LoginRequest(
    @NotBlank(message = "email must not be blank")
        @Size(max = 254, message = "email is too long")
        @Pattern(regexp = ValidationPatterns.EMAIL, message = "email format is invalid")
        String email,
    @NotBlank(message = "password must not be blank")
        @Size(min = 8, max = 128, message = "password length is invalid")
        String password) {}
