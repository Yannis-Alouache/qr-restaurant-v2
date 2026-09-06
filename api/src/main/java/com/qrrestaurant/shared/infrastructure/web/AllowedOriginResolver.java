package com.qrrestaurant.shared.infrastructure.web;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
public class AllowedOriginResolver {

    private final String clientBaseUrl;
    private final String adminBaseUrl;
    private final List<String> extraOriginPatterns;

    public AllowedOriginResolver(@Value("${app.client-base-url}") String clientBaseUrl,
                                 @Value("${app.admin-base-url}") String adminBaseUrl,
                                 @Value("${app.cors.extra-origin-patterns:}") String extraOriginPatterns) {
        this.clientBaseUrl = clientBaseUrl;
        this.adminBaseUrl = adminBaseUrl;
        this.extraOriginPatterns = Arrays.stream(extraOriginPatterns.split(","))
                .map(String::trim)
                .filter(pattern -> !pattern.isBlank())
                .toList();
    }

    /**
     * Origines autorisées pour CORS et la poignée de main WebSocket/SockJS. Les
     * entrées peuvent être des origines exactes ou des patterns avec joker
     * (ex. {@code https://*.trycloudflare.com} pour les tunnels de dev), à
     * consommer via {@code setAllowedOriginPatterns}.
     */
    public List<String> resolve() {
        Set<String> origins = new LinkedHashSet<>();
        addOrigin(origins, clientBaseUrl);
        addOrigin(origins, adminBaseUrl);
        origins.add("http://localhost:4200");
        origins.add("http://localhost:4300");
        origins.add("http://127.0.0.1:4200");
        origins.add("http://127.0.0.1:4300");
        origins.addAll(extraOriginPatterns);
        return List.copyOf(origins);
    }

    private void addOrigin(Set<String> origins, String value) {
        try {
            URI uri = URI.create(value);
            if (uri.getScheme() != null && uri.getHost() != null) {
                origins.add(uri.getScheme() + "://" + uri.getHost() + (uri.getPort() > 0 ? ":" + uri.getPort() : ""));
            }
        } catch (IllegalArgumentException ignored) {
            // Ignore invalid configured origins to keep startup safe in local development.
        }
    }
}
