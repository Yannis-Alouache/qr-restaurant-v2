package com.qrrestaurant.auth.presentation;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Builds the httpOnly {@code jwt} cookie. Presentation-only concern: translating
 * the application-issued token into an HTTP {@code Set-Cookie} header. Cookie
 * attributes mirror the JWT expiration so the cookie and the token die together.
 */
@Component
public class JwtCookieFactory {

    static final String COOKIE_NAME = "jwt";

    private final boolean secure;
    private final String sameSite;
    private final Duration expiration;

    public JwtCookieFactory(@Value("${jwt.cookie.secure:false}") boolean secure,
                            @Value("${jwt.cookie.same-site:Lax}") String sameSite,
                            @Value("${jwt.expiration:86400000}") long expirationMs) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.expiration = Duration.ofMillis(expirationMs);
    }

    /** Session cookie carrying a freshly issued token; lives as long as the token. */
    public ResponseCookie session(String token) {
        return base().value(token).maxAge(expiration).build();
    }

    /** Expired cookie used by logout to instruct the browser to drop the token. */
    public ResponseCookie expired() {
        return base().value("").maxAge(Duration.ZERO).build();
    }

    private ResponseCookie.ResponseCookieBuilder base() {
        return ResponseCookie.from(COOKIE_NAME)
                .httpOnly(true)
                .secure(secure)
                .sameSite(sameSite)
                .path("/");
    }
}
