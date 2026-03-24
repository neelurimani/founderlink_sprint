package com.founderlink.userservice.entities;

import java.util.Locale;

public enum UserRole {
    ROLE_FOUNDER,
    ROLE_INVESTOR,
    ROLE_COFUNDER,
    ROLE_ADMIN;

    public static UserRole fromValue(String role) {
        if (role == null || role.isBlank()) {
            throw new IllegalArgumentException("role must not be blank");
        }

        String normalized = role.trim().toUpperCase(Locale.ROOT);
        if (normalized.startsWith("ROLE_")) {
            normalized = normalized.substring("ROLE_".length());
        }
        normalized = normalized.replace("-", "").replace("_", "").replace(" ", "");

        return switch (normalized) {
            case "FOUNDER" -> ROLE_FOUNDER;
            case "INVESTOR" -> ROLE_INVESTOR;
            case "COFOUNDER", "COFUNDER" -> ROLE_COFUNDER;
            case "ADMIN" -> ROLE_ADMIN;
            default -> throw new IllegalArgumentException("Unsupported role " + role);
        };
    }
}
