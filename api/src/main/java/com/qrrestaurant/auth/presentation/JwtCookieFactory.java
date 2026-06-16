package com.qrrestaurant.auth.presentation;

import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Source unique de la politique du cookie de session {@code jwt} : httpOnly
 * (jamais lisible par le JS), {@code SameSite=Lax} (mitigation CSRF des
 * requêtes modifiant l'état), {@code Path=/}. L'attribut {@code Secure} est
 * adaptatif : posé uniquement quand la requête courante est en HTTPS afin de
 * rester fonctionnel en local sur HTTP (cf. issue #4).
 */
@Component
public class JwtCookieFactory {

    static final String COOKIE_NAME = "jwt";
    private static final String SAME_SITE = "Lax";
    private static final String PATH = "/";

    ResponseCookie issue(String token, long expiresInSeconds, boolean secure) {
        return base(secure)
                .value(token)
                .maxAge(expiresInSeconds)
                .build();
    }

    ResponseCookie clear(boolean secure) {
        return base(secure)
                .value("")
                .maxAge(0)
                .build();
    }

    private ResponseCookie.ResponseCookieBuilder base(boolean secure) {
        return ResponseCookie.from(COOKIE_NAME)
                .httpOnly(true)
                .secure(secure)
                .path(PATH)
                .sameSite(SAME_SITE);
    }
}
