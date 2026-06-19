package com.qrrestaurant.auth.application.dto;

/** HTTP response body for auth endpoints. Never carries the token — it lives in the httpOnly cookie. */
public record AuthResponse(String userId) {}
