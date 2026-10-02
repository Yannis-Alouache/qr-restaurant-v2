package com.qrrestaurant.auth.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class PasswordResetToken {

    private final UUID id;
    private final UUID userId;
    private final String tokenHash;
    private final LocalDateTime expiresAt;
    private final LocalDateTime createdAt;
    private LocalDateTime usedAt;

    private PasswordResetToken(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt,
                               LocalDateTime createdAt, LocalDateTime usedAt) {
        this.id = id;
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.usedAt = usedAt;
    }

    public static PasswordResetToken issue(UUID userId, String tokenHash, LocalDateTime now, LocalDateTime expiresAt) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(tokenHash, "tokenHash");
        if (!now.isBefore(expiresAt)) {
            throw new IllegalArgumentException("Un jeton de réinitialisation doit expirer après sa création");
        }
        return new PasswordResetToken(null, userId, tokenHash, expiresAt, now, null);
    }

    public static PasswordResetToken from(UUID id, UUID userId, String tokenHash, LocalDateTime expiresAt,
                                          LocalDateTime createdAt, LocalDateTime usedAt) {
        return new PasswordResetToken(id, userId, tokenHash, expiresAt, createdAt, usedAt);
    }

    public boolean isExpired(LocalDateTime now) {
        return !now.isBefore(expiresAt);
    }

    public boolean isUsed() {
        return usedAt != null;
    }

    /** Un jeton n'est consommable qu'une seule fois et seulement avant son expiration. */
    public void assertUsable(LocalDateTime now) {
        if (isUsed() || isExpired(now)) {
            throw new InvalidResetTokenException();
        }
    }

    public void markUsed(LocalDateTime now) {
        assertUsable(now);
        usedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getTokenHash() { return tokenHash; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUsedAt() { return usedAt; }
}
