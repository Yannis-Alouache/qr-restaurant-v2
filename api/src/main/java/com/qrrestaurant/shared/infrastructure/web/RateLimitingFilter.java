package com.qrrestaurant.shared.infrastructure.web;

import com.qrrestaurant.shared.presentation.ApiErrorResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Duration WINDOW = Duration.ofMinutes(1);
    private static final List<Rule> RULES = List.of(
            new Rule("POST", "/api/auth/", 20),
            new Rule("POST", "/api/public/orders", 30),
            new Rule("POST", "/api/public/payments/checkout", 20),
            new Rule("POST", "/api/webhooks/stripe", 60)
    );

    private final Map<String, CounterWindow> counters = new ConcurrentHashMap<>();
    private final AtomicInteger requestCount = new AtomicInteger();
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Autowired
    public RateLimitingFilter(ObjectMapper objectMapper) {
        this(objectMapper, Clock.systemUTC());
    }

    RateLimitingFilter(ObjectMapper objectMapper, Clock clock) {
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return findRule(request).isEmpty();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Rule rule = findRule(request).orElseThrow();
        Instant now = clock.instant();
        String key = rule.method() + ":" + rule.pathPrefix() + ":" + resolveClientIp(request);

        CounterWindow counter = counters.computeIfAbsent(key, ignored -> new CounterWindow(now));
        if (!counter.tryAcquire(now, rule.limit())) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    new ApiErrorResponse("Trop de requêtes, veuillez réessayer plus tard", HttpStatus.TOO_MANY_REQUESTS.value()));
            return;
        }

        pruneExpiredCounters(now);
        filterChain.doFilter(request, response);
    }

    private Optional<Rule> findRule(HttpServletRequest request) {
        return RULES.stream()
                .filter(rule -> rule.matches(request.getMethod(), request.getRequestURI()))
                .findFirst();
    }

    private String resolveClientIp(HttpServletRequest request) {
        return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                .map(value -> value.split(",")[0].trim())
                .filter(value -> !value.isBlank())
                .orElse(request.getRemoteAddr());
    }

    private void pruneExpiredCounters(Instant now) {
        if (requestCount.incrementAndGet() % 100 != 0) {
            return;
        }

        counters.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private record Rule(String method, String pathPrefix, int limit) {
        boolean matches(String requestMethod, String requestPath) {
            return method.equalsIgnoreCase(requestMethod) && requestPath.startsWith(pathPrefix);
        }
    }

    private static final class CounterWindow {
        private Instant windowStart;
        private int count;

        private CounterWindow(Instant windowStart) {
            this.windowStart = windowStart;
        }

        private synchronized boolean tryAcquire(Instant now, int limit) {
            if (shouldReset(now)) {
                windowStart = now;
                count = 0;
            }

            if (count >= limit) {
                return false;
            }

            count += 1;
            return true;
        }

        private synchronized boolean isExpired(Instant now) {
            return now.isAfter(windowStart.plus(WINDOW).plus(WINDOW));
        }

        private synchronized boolean shouldReset(Instant now) {
            return now.isAfter(windowStart.plus(WINDOW));
        }
    }
}
