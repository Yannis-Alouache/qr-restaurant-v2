package com.qrrestaurant.auth.application.dto;

/**
 * Application-layer result of an authentication flow: the freshly issued token
 * plus the authenticated user id. The token is handed to the presentation layer,
 * which attaches it as an httpOnly cookie — it never reaches the HTTP body.
 */
public record AuthSession(String token, String userId) {}
