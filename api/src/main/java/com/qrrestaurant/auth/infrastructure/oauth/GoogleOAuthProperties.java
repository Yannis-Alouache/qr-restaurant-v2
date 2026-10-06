package com.qrrestaurant.auth.infrastructure.oauth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Paramètres du client OAuth2 Google. Lu via {@code @Value} (et non
 * {@code spring.security.oauth2.client.*}) pour pouvoir conditionner le
 * câblage entier à la présence d'un client-id : sans clé configurée, aucun
 * endpoint ni bean Google n'est enregistré et le frontend masque le bouton.
 *
 * <p>{@code redirect-uri} doit être l'URI exactement déclarée dans Google
 * Cloud Console. En local comme en production, la danse OAuth passe par le
 * proxy du frontend (Angular dev proxy / nginx) qui transmet {@code /api}
 * vers l'API — d'où le défaut {@code http://localhost:4200/...} côté dev.</p>
 */
@Component
public class GoogleOAuthProperties {

    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;

    public GoogleOAuthProperties(@Value("${app.google.client-id:}") String clientId,
                                 @Value("${app.google.client-secret:}") String clientSecret,
                                 @Value("${app.google.redirect-uri:http://localhost:4200/api/auth/oauth2/code/google}") String redirectUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
    }

    /** Google n'est activé que si un client-id est configuré. */
    public boolean isEnabled() {
        return clientId != null && !clientId.isBlank();
    }

    public String getClientId() { return clientId; }
    public String getClientSecret() { return clientSecret; }
    public String getRedirectUri() { return redirectUri; }
}
