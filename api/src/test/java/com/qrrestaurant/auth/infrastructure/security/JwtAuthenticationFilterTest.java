package com.qrrestaurant.auth.infrastructure.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class JwtAuthenticationFilterTest {

    private static final UUID USER_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final JwtService jwtService =
            new JwtService("test-secret-with-at-least-32-bytes-long-enough", 86_400_000L);
    private final JwtAuthenticationFilter filter = new JwtAuthenticationFilter(jwtService);

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void authenticatesPrincipalWhenJwtCookieIsValid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt", jwtService.generateToken(USER_ID, "user@example.com")));

        boolean[] chainContinued = {false};
        FilterChain chain = (req, res) -> chainContinued[0] = true;

        filter.doFilter(request, new MockHttpServletResponse(), chain);

        var auth = SecurityContextHolder.getContext().getAuthentication();
        assertEquals(USER_ID, auth.getPrincipal());
        assertEquals(true, chainContinued[0]);
    }

    @Test
    void doesNotAuthenticateWhenJwtCookieIsAbsent() throws Exception {
        filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }

    @Test
    void doesNotAuthenticateWhenJwtCookieIsTampered() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt", "not-a-valid-token"));

        filter.doFilter(request, new MockHttpServletResponse(), (req, res) -> {});

        assertNull(SecurityContextHolder.getContext().getAuthentication());
    }
}
