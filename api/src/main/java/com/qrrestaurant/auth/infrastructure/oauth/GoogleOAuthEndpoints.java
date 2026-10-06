package com.qrrestaurant.auth.infrastructure.oauth;

/** URIs des endpoints OAuth2 de l'API, servis sous {@code /api/auth} pour traverser les proxys frontend. */
public final class GoogleOAuthEndpoints {

    /** Début de la danse OAuth : /api/auth/oauth2/authorization/google. */
    public static final String AUTHORIZATION_BASE_URI = "/api/auth/oauth2/authorization";

    /** Retour de Google avec le code d'autorisation. */
    public static final String REDIRECTION_BASE_URI = "/api/auth/oauth2/code/*";

    private GoogleOAuthEndpoints() {
    }
}
