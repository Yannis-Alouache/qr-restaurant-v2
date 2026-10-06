package com.qrrestaurant.auth.infrastructure.oauth;

import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.auth.application.dto.AuthSession;
import com.qrrestaurant.auth.presentation.JwtCookieFactory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

/**
 * Retour réussi de Google : émission de la session maison (JWT en cookie
 * httpOnly, même mécanique que /api/auth/login) puis redirection vers le
 * back-office. Le token ne transite jamais dans l'URL de redirection.
 */
@Component
public class GoogleAuthSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    static final String ERROR_REDIRECT_PATH = "/login";
    static final String ERROR_QUERY_PARAM = "erreur=google";

    private final AuthService authService;
    private final JwtCookieFactory cookieFactory;
    private final String adminBaseUrl;

    public GoogleAuthSuccessHandler(AuthService authService,
                                    JwtCookieFactory cookieFactory,
                                    @Value("${app.admin-base-url:http://localhost:4200}") String adminBaseUrl) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
        this.adminBaseUrl = adminBaseUrl;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        if (!(authentication.getPrincipal() instanceof OidcUser oidcUser)
                || oidcUser.getEmail() == null
                || !Boolean.TRUE.equals(oidcUser.getEmailVerified())) {
            redirectOnError(request, response);
            return;
        }

        try {
            AuthSession session = authService.loginWithGoogle(oidcUser.getEmail(), true);
            response.addHeader(HttpHeaders.SET_COOKIE, cookieFactory.session(session.token()).toString());
        } catch (AuthService.GoogleEmailNotVerifiedException e) {
            redirectOnError(request, response);
            return;
        }

        getRedirectStrategy().sendRedirect(request, response, adminBaseUrl + "/");
    }

    private void redirectOnError(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String target = UriComponentsBuilder.fromHttpUrl(adminBaseUrl)
                .path(ERROR_REDIRECT_PATH)
                .query(ERROR_QUERY_PARAM)
                .toUriString();
        getRedirectStrategy().sendRedirect(request, response, target);
    }
}
