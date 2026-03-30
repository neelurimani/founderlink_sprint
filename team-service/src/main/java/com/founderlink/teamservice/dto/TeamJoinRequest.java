package com.founderlink.teamservice.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record TeamJoinRequest(@NotNull @Positive Long startupId) {}
