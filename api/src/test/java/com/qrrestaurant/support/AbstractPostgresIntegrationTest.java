package com.qrrestaurant.support;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;

import java.util.UUID;

@SpringBootTest
public abstract class AbstractPostgresIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    protected static final UUID RESTAURANT_ID = UUID.fromString("b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22");
    protected static final UUID OWNER_ID = UUID.fromString("a0eebc99-9c0b-4ef8-bb6d-6bb9bd380a11");
    protected static final UUID TABLE_1_ID = UUID.fromString("c0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01");
    protected static final UUID BURGER_MENU_ID = UUID.fromString("e0eebc99-0002-4ef8-bb6d-6bb9bd380a01");
    protected static final UUID BACON_MENU_ID = UUID.fromString("e0eebc99-0002-4ef8-bb6d-6bb9bd380a02");
    protected static final UUID FRIES_ID = UUID.fromString("f0eebc99-0001-4ef8-bb6d-6bb9bd380a01");
    protected static final UUID NUGGETS_ID = UUID.fromString("f0eebc99-0001-4ef8-bb6d-6bb9bd380a02");
    protected static final UUID COKE_ID = UUID.fromString("f0eebc99-0001-4ef8-bb6d-6bb9bd380a03");
    protected static final UUID BROWNIE_ID = UUID.fromString("e0eebc99-0001-4ef8-bb6d-6bb9bd380a03");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("stripe.secret-key", () -> "sk_test_dummy");
        registry.add("stripe.webhook-secret", () -> "whsec_test");
        registry.add("app.client-base-url", () -> "http://localhost:4300");
        registry.add("app.admin-base-url", () -> "http://localhost:4200");
    }
}
