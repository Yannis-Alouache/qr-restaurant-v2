package com.qrrestaurant.auth.application.dto;

/**
 * Résultat interne d'une authentification : tout ce dont la couche présentation
 * a besoin pour émettre le cookie de session {@code jwt} et construire la
 * réponse ({@link AuthResponse}). Type application, jamais sérialisé tel quel
 * vers le client : le token ne doit pas fuiter hors du cookie httpOnly.
 *
 * @param token             le JWT à placer dans le cookie httpOnly
 * @param userId            identifiant de l'utilisateur authentifié
 * @param expiresInSeconds  durée de validité, alignée sur l'expiration du JWT
 */
public record AuthResult(String token, String userId, long expiresInSeconds) {}
