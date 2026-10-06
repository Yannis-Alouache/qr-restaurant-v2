package com.qrrestaurant.auth.infrastructure.oauth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Échec de la danse OAuth (refus utilisateur chez Google, state invalide,
 * code expiré…) : retour au back-office sur la page de login avec un paramètre
 * d'erreur que le SPA affiche. Aucun détail technique n'est exposé dans l'URL.
 */
@Component
public class GoogleAuthFailureHandler extends SimpleUrlAuthenticationFailureHandler {

    private final String adminBaseUrl;

    public GoogleAuthFailureHandler(@Value("${app.admin-base-url:http://localhost:4200}") String adminBaseUrl) {
        this.adminBaseUrl = adminBaseUrl;
    }

    @Override
    public void onAuthenticationFailure(HttpServletRequest request,
                                        HttpServletResponse response,
                                        AuthenticationException exception) throws IOException {
        String target = UriComponentsBuilder.fromHttpUrl(adminBaseUrl)
                .path(GoogleAuthSuccessHandler.ERROR_REDIRECT_PATH)
                .query(GoogleAuthSuccessHandler.ERROR_QUERY_PARAM)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
