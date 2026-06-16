package com.qrrestaurant.auth.domain;

import java.util.UUID;

public interface TokenService {
    String generateToken(UUID userId, String email);

    /**
     * Durée de validité des tokens émis, en millisecondes. Source de vérité pour
     * aligner l'expiration du cookie de session sur celle du JWT (cf. issue #4).
     */
    long expirationMillis();
}
