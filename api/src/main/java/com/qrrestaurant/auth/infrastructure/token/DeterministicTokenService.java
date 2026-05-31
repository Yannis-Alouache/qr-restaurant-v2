package com.qrrestaurant.auth.infrastructure.token;

import com.qrrestaurant.auth.domain.TokenService;

import java.util.UUID;

public class DeterministicTokenService implements TokenService {
    @Override
    public String generateToken(UUID userId, String email) {
        return "token:%s:%s".formatted(userId, email);
    }
}
