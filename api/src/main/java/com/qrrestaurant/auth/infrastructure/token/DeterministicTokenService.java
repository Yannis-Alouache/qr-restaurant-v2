package com.qrrestaurant.auth.infrastructure.token;

import com.qrrestaurant.auth.domain.TokenService;

import java.util.UUID;

public class DeterministicTokenService implements TokenService {

    private static final long EXPIRATION_MILLIS = 86_400_000L;

    @Override
    public String generateToken(UUID userId, String email) {
        return "token:%s:%s".formatted(userId, email);
    }

    @Override
    public long expirationMillis() {
        return EXPIRATION_MILLIS;
    }
}
