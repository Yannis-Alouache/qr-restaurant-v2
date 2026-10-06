package com.qrrestaurant.shared.infrastructure.config;
import com.qrrestaurant.shared.infrastructure.web.AllowedOriginResolver;
import com.qrrestaurant.shared.infrastructure.web.RateLimitingFilter;

import com.qrrestaurant.auth.infrastructure.oauth.GoogleAuthFailureHandler;
import com.qrrestaurant.auth.infrastructure.oauth.GoogleAuthSuccessHandler;
import com.qrrestaurant.auth.infrastructure.oauth.GoogleOAuthEndpoints;
import com.qrrestaurant.auth.infrastructure.oauth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.qrrestaurant.auth.infrastructure.security.JwtAuthenticationFilter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final RateLimitingFilter rateLimitingFilter;
    private final AllowedOriginResolver allowedOriginResolver;
    private final ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository;
    private final GoogleAuthSuccessHandler googleAuthSuccessHandler;
    private final GoogleAuthFailureHandler googleAuthFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;

    public SecurityConfig(JwtAuthenticationFilter jwtAuthenticationFilter,
                          RateLimitingFilter rateLimitingFilter,
                          AllowedOriginResolver allowedOriginResolver,
                          ObjectProvider<ClientRegistrationRepository> clientRegistrationRepository,
                          GoogleAuthSuccessHandler googleAuthSuccessHandler,
                          GoogleAuthFailureHandler googleAuthFailureHandler,
                          HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.rateLimitingFilter = rateLimitingFilter;
        this.allowedOriginResolver = allowedOriginResolver;
        this.clientRegistrationRepository = clientRegistrationRepository;
        this.googleAuthSuccessHandler = googleAuthSuccessHandler;
        this.googleAuthFailureHandler = googleAuthFailureHandler;
        this.cookieAuthorizationRequestRepository = cookieAuthorizationRequestRepository;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/auth/me").authenticated()
                .requestMatchers("/api/public/**").permitAll()
                .requestMatchers("/api/auth/**").permitAll()
                .requestMatchers("/api/webhooks/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/images/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**").permitAll()
                // permitAll nécessaire : le handshake doit rester accessible aux
                // invités (suivi de commande sans compte). L'autorisation réelle
                // se fait au niveau STOMP, par souscription — cf. WebSocketConfig
                // et WsSubscriptionSecurityInterceptor.
                .requestMatchers("/ws/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex.authenticationEntryPoint((request, response, authException) -> {
                response.setStatus(HttpStatus.UNAUTHORIZED.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.getWriter().write("{\"statusCode\":401,\"message\":\"Authentification requise\"}");
            }))
            .addFilterBefore(rateLimitingFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        // Connexion Google : activée seulement si un client OAuth2 est configuré
        // (bean ClientRegistrationRepository conditionnel — cf. GoogleOAuthClientConfig).
        // La requête d'autorisation vit dans un cookie, pas en session : l'API
        // reste stateless. Le succès/échec réémettent la session maison (JWT en
        // cookie) et redirigent le navigateur vers le back-office.
        ClientRegistrationRepository googleClient = clientRegistrationRepository.getIfAvailable();
        if (googleClient != null) {
            http.oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(authorization -> authorization
                    .baseUri(GoogleOAuthEndpoints.AUTHORIZATION_BASE_URI)
                    .authorizationRequestRepository(cookieAuthorizationRequestRepository))
                .redirectionEndpoint(redirection -> redirection
                    .baseUri(GoogleOAuthEndpoints.REDIRECTION_BASE_URI))
                .successHandler(googleAuthSuccessHandler)
                .failureHandler(googleAuthFailureHandler)
            );
        }

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        // Patterns (et non origines exactes) pour supporter les jokers du
        // AllowedOriginResolver — cf. WebSocketConfig, même mécanique.
        config.setAllowedOriginPatterns(allowedOriginResolver.resolve());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Stripe-Signature"));
        config.setExposedHeaders(List.of("Location"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/ws", config);
        source.registerCorsConfiguration("/ws/**", config);
        return source;
    }
}
