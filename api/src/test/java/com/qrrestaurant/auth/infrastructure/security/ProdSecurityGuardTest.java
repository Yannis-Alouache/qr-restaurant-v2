package com.qrrestaurant.auth.infrastructure.security;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProdSecurityGuardTest {

    private static final String STRONG_SECRET = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";

    @Test
    void refusesASecretShorterThan256Bits() {
        String shortSecret = "trop-court";
        IllegalStateException thrown = assertThrows(IllegalStateException.class,
                () -> new ProdSecurityGuard(shortSecret, true).afterPropertiesSet());
        assertTrue(thrown.getMessage().contains("trop court"));
    }

    @Test
    void refusesTheKnownDevelopmentSecret() {
        assertThrows(IllegalStateException.class,
                () -> new ProdSecurityGuard(ProdSecurityGuard.KNOWN_DEV_SECRET, true).afterPropertiesSet());
    }

    @Test
    void refusesTheRepositoryPlaceholderSecret() {
        assertThrows(IllegalStateException.class,
                () -> new ProdSecurityGuard(ProdSecurityGuard.KNOWN_PLACEHOLDER_SECRET, true).afterPropertiesSet());
    }

    @Test
    void acceptsAStrongRandomSecret() {
        assertDoesNotThrow(() -> new ProdSecurityGuard(STRONG_SECRET, true).afterPropertiesSet());
    }

    @Test
    void acceptsInsecureCookieButOnlyAfterExplicitOverride() {
        // La valeur non sécurisée ne fait qu'émettre un WARN, elle ne bloque pas
        // le démarrage : override volontaire possible derrière un TLS amont.
        assertDoesNotThrow(() -> new ProdSecurityGuard(STRONG_SECRET, false).afterPropertiesSet());
    }
}
