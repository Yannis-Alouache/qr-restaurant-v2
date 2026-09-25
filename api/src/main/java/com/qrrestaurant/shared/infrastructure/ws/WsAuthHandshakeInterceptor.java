package com.qrrestaurant.shared.infrastructure.ws;

import com.qrrestaurant.auth.infrastructure.security.JwtService;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;
import org.springframework.web.util.WebUtils;

import java.util.Map;
import java.util.UUID;

/**
 * Résout l'identité à l'ouverture de la connexion WebSocket via le cookie JWT
 * (même mécanique que {@code JwtAuthenticationFilter} sur le pipeline HTTP).
 * <p>
 * La connexion anonyme est volontairement acceptée : les invités suivent leur
 * commande sans compte. L'autorisation fine se joue à la souscription, dans
 * {@link WsSubscriptionSecurityInterceptor}.
 */
@Component
public class WsAuthHandshakeInterceptor implements HandshakeInterceptor {

    public static final String USER_ID_ATTRIBUTE = "wsUserId";
    private static final String COOKIE_NAME = "jwt";

    private final JwtService jwtService;

    public WsAuthHandshakeInterceptor(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public boolean beforeHandshake(ServerHttpRequest request,
                                   ServerHttpResponse response,
                                   WebSocketHandler wsHandler,
                                   Map<String, Object> attributes) {
        if (request instanceof ServletServerHttpRequest servletRequest) {
            var cookie = WebUtils.getCookie(servletRequest.getServletRequest(), COOKIE_NAME);
            if (cookie != null && jwtService.isTokenValid(cookie.getValue())) {
                UUID userId = jwtService.extractUserId(cookie.getValue());
                attributes.put(USER_ID_ATTRIBUTE, userId);
            }
        }
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request,
                               ServerHttpResponse response,
                               WebSocketHandler wsHandler,
                               Exception exception) {
        // Rien à faire : l'identité est déjà posée en attribut de session.
    }
}
