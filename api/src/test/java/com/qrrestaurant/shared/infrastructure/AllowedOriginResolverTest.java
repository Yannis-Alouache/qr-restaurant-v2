package com.qrrestaurant.shared.infrastructure;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AllowedOriginResolverTest {

    @Test
    void shouldExposeConfiguredDeploymentOriginsAlongsideTheLocalDevelopmentOnes() {
        AllowedOriginResolver resolver = new AllowedOriginResolver(
                "https://client.qr-restaurant.example/menu",
                "https://admin.qr-restaurant.example:4443/backoffice"
        );

        assertEquals(
                java.util.List.of(
                        "https://client.qr-restaurant.example",
                        "https://admin.qr-restaurant.example:4443",
                        "http://localhost:4200",
                        "http://localhost:4300",
                        "http://127.0.0.1:4200",
                        "http://127.0.0.1:4300"
                ),
                resolver.resolve()
        );
    }
}
