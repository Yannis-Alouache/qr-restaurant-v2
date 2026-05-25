package com.qrrestaurant.shared.presentation;

import java.time.Instant;
import java.util.Map;

public record ApiErrorResponse(
        String error,
        String message,
        int status,
        Instant timestamp,
        Map<String, String> details
) {

    public ApiErrorResponse(String message, int status) {
        this(message, message, status, Instant.now(), Map.of());
    }

    public ApiErrorResponse(String message, int status, Map<String, String> details) {
        this(message, message, status, Instant.now(), details);
    }
}
