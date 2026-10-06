package com.qrrestaurant.auth.infrastructure.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Hachage des mots de passe des comptes restaurateurs.
 *
 * <p>Délibérément hors de {@code SecurityConfig} : celle-ci dépend désormais
 * des handlers de connexion Google, qui dépendent d'AuthService, qui a besoin
 * de ce bean — défini dans SecurityConfig, il créerait un cycle
 * SecurityConfig → handlers → AuthService → PasswordEncoder → SecurityConfig.</p>
 */
@Configuration
public class PasswordConfig {

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
