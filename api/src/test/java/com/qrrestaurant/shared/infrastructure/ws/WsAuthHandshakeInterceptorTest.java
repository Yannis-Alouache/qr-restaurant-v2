package com.qrrestaurant.shared.infrastructure.ws;

import com.qrrestaurant.auth.infrastructure.security.JwtService;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WsAuthHandshakeInterceptorTest {

    private static final String TEST_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    private final JwtService jwtService = new JwtService(TEST_SECRET, 3_600_000);
    private final WsAuthHandshakeInterceptor interceptor = new WsAuthHandshakeInterceptor(jwtService);

    @Test
    void storesUserIdInAttributesWhenJwtCookieIsValid() {
        UUID userId = UUID.randomUUID();
        String token = jwtService.generateToken(userId, "owner@test.com");
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(requestWithCookie(token)),
                null, new TextWebSocketHandler(), attributes);

        assertTrue(accepted);
        assertEquals(userId, attributes.get(WsAuthHandshakeInterceptor.USER_ID_ATTRIBUTE));
    }

    @Test
    void acceptsAnonymousHandshakeWithoutCookie() {
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(new MockHttpServletRequest()),
                null, new TextWebSocketHandler(), attributes);

        assertTrue(accepted);
        assertTrue(attributes.isEmpty());
    }

    @Test
    void ignoresInvalidTokenAndStillAcceptsTheConnection() {
        Map<String, Object> attributes = new HashMap<>();

        boolean accepted = interceptor.beforeHandshake(
                new ServletServerHttpRequest(requestWithCookie("not-a-jwt")),
                null, new TextWebSocketHandler(), attributes);

        assertTrue(accepted);
        assertTrue(attributes.isEmpty());
    }

    private MockHttpServletRequest requestWithCookie(String token) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("jwt", token));
        return request;
    }
}
