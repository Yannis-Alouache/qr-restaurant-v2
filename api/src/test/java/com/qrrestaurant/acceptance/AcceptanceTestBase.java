package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.payment.infrastructure.gateway.DeterministicPaymentGateway;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import com.stripe.Stripe;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
@Import(AcceptanceTestBase.TestConfig.class)
abstract class AcceptanceTestBase extends AbstractPostgresIntegrationTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    JdbcTemplate jdbcTemplate;

    // ── HTTP helpers ──────────────────────────────────────────────────

    JsonNode postJson(String path, String payload,
                      org.springframework.test.web.servlet.ResultMatcher expectedStatus) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(expectedStatus)
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    JsonNode getJson(String path) throws Exception {
        MvcResult result = mockMvc.perform(get(path))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    JsonNode postAuthorizedJson(String path, String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    JsonNode getAuthorizedJson(String path, String token) throws Exception {
        MvcResult result = mockMvc.perform(get(path)
                        .header("Authorization", token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    JsonNode putAuthorizedJson(String path, String token, String payload) throws Exception {
        MvcResult result = mockMvc.perform(put(path)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    void patchAuthorizedStatus(String path, String token, String nextStatus) throws Exception {
        mockMvc.perform(patch(path)
                        .header("Authorization", token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "status": "%s"
                                }
                                """.formatted(nextStatus)))
                .andExpect(status().isNoContent());
    }

    // ── Seed data helpers ─────────────────────────────────────────────

    void restoreSeedDemoState() {
        jdbcTemplate.update(
                """
                UPDATE restaurant
                SET name = ?, address = ?, theme_id = ?, payment_provider_account_id = ?
                WHERE id = ?
                """,
                "Naia Burger",
                "12 Rue de la Paix, Paris",
                "chaud",
                "acct_seed_test",
                RESTAURANT_ID);

        jdbcTemplate.update(
                """
                UPDATE menu_item
                SET available = true
                WHERE id IN (?, ?, ?, ?, ?, ?)
                """,
                BURGER_MENU_ID,
                BACON_MENU_ID,
                FRIES_ID,
                NUGGETS_ID,
                COKE_ID,
                BROWNIE_ID);
    }

    JsonNode createSeedDemoOrder() throws Exception {
        UUID comboGroupId = UUID.randomUUID();
        return postJson("/api/public/orders", """
                {
                  "slug": "naia-burger",
                  "tableId": "%s",
                  "items": [
                    {
                      "menuItemId": "%s",
                      "quantity": 2,
                      "menuGroupId": "%s",
                      "menuRole": "plat"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 2,
                      "menuGroupId": "%s",
                      "menuRole": "accompagnement"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 2,
                      "menuGroupId": "%s",
                      "menuRole": "boisson"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1
                    }
                  ]
                }
                """.formatted(TABLE_1_ID, BURGER_MENU_ID, comboGroupId, FRIES_ID, comboGroupId, COKE_ID, comboGroupId, BROWNIE_ID),
                status().isCreated());
    }

    String seedOwnerBearerToken() throws Exception {
        JsonNode login = postJson("/api/auth/login", """
                {
                  "email": "owner@test.com",
                  "password": "Secret123!"
                }
                """, status().isOk());
        return "Bearer " + login.path("token").asText();
    }

    // ── Assertion helpers ─────────────────────────────────────────────

    boolean containsMenuItem(JsonNode publicMenu, String itemId) {
        for (JsonNode category : publicMenu.path("categories")) {
            for (JsonNode item : category.path("items")) {
                if (itemId.equals(item.path("id").asText())) {
                    return true;
                }
            }
        }
        return false;
    }

    boolean containsComposition(JsonNode publicMenu, String itemId) {
        for (JsonNode composition : publicMenu.path("compositions")) {
            if (itemId.equals(composition.path("menuItemId").asText())) {
                return true;
            }
        }
        return false;
    }

    boolean containsOrder(JsonNode orders, String orderId) {
        return findOrder(orders, orderId) != null;
    }

    JsonNode findOrder(JsonNode orders, String orderId) {
        for (JsonNode order : orders) {
            if (orderId.equals(order.path("id").asText())) {
                return order;
            }
        }
        return null;
    }

    // ── Stripe helpers ────────────────────────────────────────────────

    void postStripeWebhook(String payload) throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());
    }

    String checkoutCompletedPayload(String orderId, String paymentIntentId) {
        return """
                {
                  "id": "evt_checkout_completed",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_completed_%s",
                      "object": "checkout.session",
                      "payment_intent": "%s",
                      "metadata": {
                        "order_id": "%s"
                      }
                    }
                  }
                }
                """.formatted(Stripe.API_VERSION, orderId, paymentIntentId, orderId);
    }

    String checkoutExpiredPayload(String orderId) {
        return """
                {
                  "id": "evt_checkout_expired",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.expired",
                  "data": {
                    "object": {
                      "id": "cs_expired_%s",
                      "object": "checkout.session",
                      "metadata": {
                        "order_id": "%s"
                      }
                    }
                  }
                }
                """.formatted(Stripe.API_VERSION, orderId, orderId);
    }

    private String stripeSignature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec("whsec_test".getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signedPayload = timestamp + "." + payload;
        String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=%d,v1=%s".formatted(timestamp, signature);
    }

    // ── Test configuration ────────────────────────────────────────────

    @TestConfiguration
    static class TestConfig {
        @Bean
        @Primary
        PaymentGateway paymentGateway() {
            return new DeterministicPaymentGateway();
        }
    }
}
