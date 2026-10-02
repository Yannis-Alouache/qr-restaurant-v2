package com.qrrestaurant.auth.domain;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetTokenTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 1, 1, 12, 0);
    private static final LocalDateTime EXPIRES_AT = NOW.plusMinutes(60);

    @Test
    void shouldBeUsableBeforeExpiration() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "hash", NOW, EXPIRES_AT);

        assertFalse(token.isUsed());
        assertFalse(token.isExpired(NOW.plusMinutes(59)));
        assertDoesNotThrow(() -> token.assertUsable(NOW.plusMinutes(59)));
    }

    @Test
    void shouldBeExpiredAfterTtl() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "hash", NOW, EXPIRES_AT);

        assertTrue(token.isExpired(EXPIRES_AT));
        assertThrows(InvalidResetTokenException.class, () -> token.assertUsable(EXPIRES_AT));
    }

    @Test
    void shouldNotBeUsableTwice() {
        PasswordResetToken token = PasswordResetToken.issue(UUID.randomUUID(), "hash", NOW, EXPIRES_AT);
        token.markUsed(NOW.plusMinutes(5));

        assertTrue(token.isUsed());
        assertThrows(InvalidResetTokenException.class, () -> token.assertUsable(NOW.plusMinutes(6)));
        assertThrows(InvalidResetTokenException.class, () -> token.markUsed(NOW.plusMinutes(6)));
    }

    @Test
    void shouldRejectIssuanceWithImmediateExpiration() {
        assertThrows(IllegalArgumentException.class,
                () -> PasswordResetToken.issue(UUID.randomUUID(), "hash", NOW, NOW));
    }
}
