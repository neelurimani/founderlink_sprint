package com.founderlink.userservice.dto;

import java.util.List;

public record ErrorResponse(
        String message,
        List<String> details
) {
    public static ErrorResponse withMessage(String message) {
        return new ErrorResponse(message, List.of());
    }
}
