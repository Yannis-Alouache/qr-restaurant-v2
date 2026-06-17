package com.qrrestaurant.shared.presentation;

public record ApiErrorResponse(
        int statusCode,
        String message
) {
}
