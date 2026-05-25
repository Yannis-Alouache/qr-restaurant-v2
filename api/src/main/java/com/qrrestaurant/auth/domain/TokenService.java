package com.qrrestaurant.auth.domain;

import java.util.UUID;

public interface TokenService {
    String generateToken(UUID userId, String email);
}
