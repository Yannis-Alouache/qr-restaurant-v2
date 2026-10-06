package com.qrrestaurant.auth.infrastructure.oauth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.client.web.AuthorizationRequestRepository;
import org.springframework.stereotype.Component;
import org.springframework.web.util.WebUtils;

import java.time.Duration;

/**
 * Stockage de la requête d'autorisation OAuth2 dans un cookie httpOnly court,
 * à la place de la session HTTP (implémentation par défaut) : l'API est
 * stateless ({@code SessionCreationPolicy.STATELESS}) et la danse OAuth doit
 * survivre à un redémarrage ou à plusieurs réplicas sans sessions collantes.
 *
 * <p>Le cookie vit 10 minutes — le temps de se rendre chez Google et d'en
 * revenir — puis expire ; un cookie corrompu est traité comme absent.</p>
 */
@Component
public class HttpCookieOAuth2AuthorizationRequestRepository
        implements AuthorizationRequestRepository<OAuth2AuthorizationRequest> {

    static final String COOKIE_NAME = "oauth2_auth_request";

    private static final Duration COOKIE_TTL = Duration.ofMinutes(10);

    private final OAuth2AuthorizationRequestCookieCodec codec = new OAuth2AuthorizationRequestCookieCodec();
    private final boolean secure;
    private final String sameSite;

    public HttpCookieOAuth2AuthorizationRequestRepository(
            @Value("${jwt.cookie.secure:false}") boolean secure,
            @Value("${jwt.cookie.same-site:Lax}") String sameSite) {
        this.secure = secure;
        this.sameSite = sameSite;
    }

    @Override
    public OAuth2AuthorizationRequest loadAuthorizationRequest(HttpServletRequest request) {
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        if (cookie == null) {
            return null;
        }

        // Ne jamais honorer un cookie dont l'état ne correspond pas au paramètre
        // renvoyé par le fournisseur : c'est la protection CSRF de la danse OAuth.
        String state = request.getParameter(OAuth2ParameterNames.STATE);
        if (state == null) {
            return null;
        }

        OAuth2AuthorizationRequest authorizationRequest = codec.decode(cookie.getValue());
        if (authorizationRequest == null || !state.equals(authorizationRequest.getState())) {
            return null;
        }
        return authorizationRequest;
    }

    @Override
    public void saveAuthorizationRequest(OAuth2AuthorizationRequest authorizationRequest,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        if (authorizationRequest == null) {
            expireCookie(response);
            return;
        }
        String value = codec.encode(authorizationRequest);
        response.addHeader(HttpHeaders.SET_COOKIE, cookie(value, COOKIE_TTL).toString());
    }

    @Override
    public OAuth2AuthorizationRequest removeAuthorizationRequest(HttpServletRequest request,
                                                                 HttpServletResponse response) {
        OAuth2AuthorizationRequest authorizationRequest = loadAuthorizationRequest(request);
        expireCookie(response);
        return authorizationRequest;
    }

    private void expireCookie(HttpServletResponse response) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie("", Duration.ZERO).toString());
    }

    private ResponseCookie cookie(String value, Duration maxAge) {
        return ResponseCookie.from(COOKIE_NAME, value)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/")
                .maxAge(maxAge)
                .build();
    }
}
