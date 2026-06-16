package com.qrrestaurant.auth.application.dto;

/**
 * Charge utile renvoyée au client après authentification. Le JWT n'y figure
 * jamais : il voyage exclusivement dans un cookie httpOnly (cf. issue #4).
 */
public record AuthResponse(String userId) {}
