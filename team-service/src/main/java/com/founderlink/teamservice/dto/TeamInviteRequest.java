package com.founderlink.teamservice.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

public record TeamInviteRequest(
    @NotNull @Positive Long startupId,
    @NotNull @Positive Long invitedUserId,
    @NotBlank @Size(max = 64) String role) {}
