package com.qrrestaurant.order.presentation;

import com.qrrestaurant.auth.infrastructure.security.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import com.qrrestaurant.support.TestAuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderAdminControllerHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldExposeTableNumberInAdminOrders() throws Exception {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO order_table (id, restaurant_id, table_id, status, total, payment_transaction_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                orderId,
                RESTAURANT_ID,
                TABLE_1_ID,
                "nouvelle",
                new BigDecimal("12.00"),
                "pi_test_123");
        jdbcTemplate.update(
                """
                INSERT INTO order_item (id, order_id, menu_item_id, name, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                orderId,
                BURGER_MENU_ID,
                "Menu Burger classique",
                1,
                new BigDecimal("12.00"));

        mockMvc.perform(get("/api/admin/orders")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].tableNumber").value(1))
                .andExpect(jsonPath("$[0].tableId").doesNotExist());
    }

    @Test
    void shouldRejectAdminPromotionOfAnUnpaidOrder() throws Exception {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO order_table (id, restaurant_id, table_id, status, total, payment_transaction_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                orderId,
                RESTAURANT_ID,
                TABLE_1_ID,
                "en_attente_paiement",
                new BigDecimal("12.00"),
                null);

        mockMvc.perform(patch("/api/admin/orders/{id}/status", orderId)
                        .cookie(ownerBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "nouvelle"
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Paiement non confirmé pour cette commande"));
    }

    private Cookie ownerBearerToken() {
        return TestAuthCookies.jwt(jwtService, OWNER_ID, "owner@test.com");
    }
}
