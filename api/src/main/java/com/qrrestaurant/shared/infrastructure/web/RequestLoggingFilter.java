package com.qrrestaurant.shared.infrastructure.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Une ligne par requête API (méthode, chemin, statut, durée) : rend le trafic
 * observable en prod sans configuration externe. Les images servies (traffic
 * élevé, caché) sont reléguées à DEBUG.
 */
@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        // Les endpoints actuator et les ressources non-API ne sont pas loggués.
        return !request.getRequestURI().startsWith("/api/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        long start = System.nanoTime();
        try {
            filterChain.doFilter(request, response);
        } finally {
            long durationMs = (System.nanoTime() - start) / 1_000_000;
            String method = request.getMethod();
            String uri = request.getRequestURI();
            int status = response.getStatus();

            if ("GET".equalsIgnoreCase(method) && uri.startsWith("/api/images/")) {
                if (log.isDebugEnabled()) {
                    log.debug("{} {} -> {} ({} ms)", method, uri, status, durationMs);
                }
            } else {
                log.info("{} {} -> {} ({} ms)", method, uri, status, durationMs);
            }
        }
    }
}
