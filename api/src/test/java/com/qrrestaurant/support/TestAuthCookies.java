package com.qrrestaurant.support;

import com.qrrestaurant.auth.infrastructure.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

/**
 * Builds the {@code jwt} cookie used to authenticate MockMvc requests, mirroring
 * what {@link com.qrrestaurant.auth.infrastructure.security.JwtAuthenticationFilter}
 * expects on the wire.
 */
public final class TestAuthCookies {

    static final String COOKIE_NAME = "jwt";
    private static final String COOKIE_PREFIX = COOKIE_NAME + "=";

    private TestAuthCookies() {}

    /** Cookie built from a freshly generated token (no real login round-trip). */
    public static Cookie jwt(JwtService jwtService, UUID userId, String email) {
        return new Cookie(COOKIE_NAME, jwtService.generateToken(userId, email));
    }

    /** Cookie read from the Set-Cookie header a real login/signup attached to a MockMvc result. */
    public static Cookie fromResult(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        String token = setCookie.split(";")[0].substring(COOKIE_PREFIX.length());
        return new Cookie(COOKIE_NAME, token);
    }
}
