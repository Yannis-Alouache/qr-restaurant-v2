package com.qrrestaurant.order.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.auth.infrastructure.security.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import com.qrrestaurant.support.TestAuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    // Le remboursement HTTP est testé jusqu'au use case : l'appel Stripe
    // réseau reste mocké (clés de test dummy).
    @MockBean
    private PaymentGateway paymentGateway;

    @Test
    void shouldExposeTableNumberInAdminOrders() throws Exception {
        UUID orderId = insertOrder("nouvelle", "pi_test_123");
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

        MvcResult result = mockMvc.perform(get("/api/admin/orders")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode order = findOrderById(new ObjectMapper()
                .readTree(result.getResponse().getContentAsString()), orderId);
        assertEquals(1, order.path("tableNumber").asInt());
        assertTrue(order.path("tableId").isMissingNode());
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

    @Test
    void shouldRefundAPaidOrderOnTheAdminBoundary() throws Exception {
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
                "pi_refund_test");

        mockMvc.perform(post("/api/admin/orders/{id}/refund", orderId)
                        .cookie(ownerBearerToken()))
                .andExpect(status().isNoContent());

        assertEquals("rembourse", jdbcTemplate.queryForObject(
                "SELECT status FROM order_table WHERE id = ?",
                String.class, orderId));
    }

    @Test
    void shouldRejectARefundOnAnUnpaidOrder() throws Exception {
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

        mockMvc.perform(post("/api/admin/orders/{id}/refund", orderId)
                        .cookie(ownerBearerToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Remboursement impossible pour une commande en statut en_attente_paiement"));
    }

    @Test
    void shouldListServedAndRefundedOrdersButNotUnpaidCarts() throws Exception {
        UUID unpaidOrderId = insertOrder("en_attente_paiement", null);
        UUID activeOrderId = insertOrder("nouvelle", "pi_active_history");
        UUID servedOrderId = insertOrder("servie", "pi_served_history");
        UUID refundedOrderId = insertOrder("rembourse", "pi_refunded_history");

        MvcResult result = mockMvc.perform(get("/api/admin/orders")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode orders = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString());

        assertEquals("nouvelle", statusOf(orders, activeOrderId));
        assertEquals("servie", statusOf(orders, servedOrderId));
        assertEquals("rembourse", statusOf(orders, refundedOrderId));
        assertFalse(containsOrder(orders, unpaidOrderId),
                "Un panier non payé ne doit pas apparaître dans le flux admin");
    }

    private UUID insertOrder(String status, String paymentTransactionId) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO order_table (id, restaurant_id, table_id, status, total, payment_transaction_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, NOW())
                """,
                orderId,
                RESTAURANT_ID,
                TABLE_1_ID,
                status,
                new BigDecimal("12.00"),
                paymentTransactionId);
        return orderId;
    }

    private static JsonNode findOrderById(JsonNode orders, UUID orderId) {
        for (JsonNode order : orders) {
            if (orderId.toString().equals(order.path("id").asText())) {
                return order;
            }
        }
        return null;
    }

    private static boolean containsOrder(JsonNode orders, UUID orderId) {
        return findOrderById(orders, orderId) != null;
    }

    private static String statusOf(JsonNode orders, UUID orderId) {
        JsonNode order = findOrderById(orders, orderId);
        if (order == null) {
            throw new AssertionError("Commande " + orderId + " absente du flux admin");
        }
        return order.path("status").asText();
    }

    private Cookie ownerBearerToken() {
        return TestAuthCookies.jwt(jwtService, OWNER_ID, "owner@test.com");
    }
}
