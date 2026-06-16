package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PaymentFlowSmokeTest extends AcceptanceTestBase {

    @Test
    void shouldCreateCheckoutSessionForSeededDemoOrder() throws Exception {
        restoreSeedDemoState();

        JsonNode createdOrder = createSeedDemoOrder();

        JsonNode checkout = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());

        assertEquals(
                "https://checkout.test/session/%s?amount=25.30&account=acct_seed_test"
                        .formatted(createdOrder.path("id").asText()),
                checkout.path("checkoutUrl").asText());

        JsonNode order = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("en_attente_paiement", order.path("status").asText());
        assertEquals(0, new BigDecimal("25.30").compareTo(order.path("total").decimalValue()));
    }

    @Test
    void shouldFinalizeSeededDemoOrderWhenStripeWebhookCompletesCheckout() throws Exception {
        restoreSeedDemoState();

        JsonNode createdOrder = createSeedDemoOrder();
        postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());

        postStripeWebhook(checkoutCompletedPayload(createdOrder.path("id").asText(), "pi_smoke_paid"));

        JsonNode order = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("nouvelle", order.path("status").asText());
        assertEquals(
                "pi_smoke_paid",
                jdbcTemplate.queryForObject(
                        "SELECT payment_transaction_id FROM order_table WHERE id = ?",
                        String.class,
                        UUID.fromString(createdOrder.path("id").asText())));
    }

    @Test
    void shouldExposeTheSeededOrderToAdminOnlyAfterPaymentIsConfirmed() throws Exception {
        restoreSeedDemoState();
        String ownerJwt = seedOwnerJwt();

        JsonNode createdOrder = createSeedDemoOrder();

        JsonNode unpaidAdminOrders = getAuthorizedJson("/api/admin/orders", ownerJwt);
        assertTrue(!containsOrder(unpaidAdminOrders, createdOrder.path("id").asText()));

        postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        postStripeWebhook(checkoutCompletedPayload(createdOrder.path("id").asText(), "pi_admin_visible"));

        JsonNode adminOrders = getAuthorizedJson("/api/admin/orders", ownerJwt);
        JsonNode paidOrder = findOrder(adminOrders, createdOrder.path("id").asText());
        assertTrue(paidOrder != null);
        assertEquals("nouvelle", paidOrder.path("status").asText());
        assertEquals(1, paidOrder.path("tableNumber").asInt());
        assertEquals(4, paidOrder.path("items").size());
    }

    @Test
    void shouldMarkSeededDemoOrderAsPaymentFailedWhenStripeWebhookExpiresCheckout() throws Exception {
        restoreSeedDemoState();

        JsonNode createdOrder = createSeedDemoOrder();
        postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());

        postStripeWebhook(checkoutExpiredPayload(createdOrder.path("id").asText()));

        JsonNode order = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("paiement_echoue", order.path("status").asText());
    }

    @Test
    void shouldAllowRetryingAStripeExpiredPaymentUntilTheOrderBecomesExploitableAgain() throws Exception {
        restoreSeedDemoState();
        String ownerJwt = seedOwnerJwt();

        JsonNode createdOrder = createSeedDemoOrder();
        postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        postStripeWebhook(checkoutExpiredPayload(createdOrder.path("id").asText()));

        JsonNode failedOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("paiement_echoue", failedOrder.path("status").asText());
        assertTrue(!containsOrder(getAuthorizedJson("/api/admin/orders", ownerJwt), createdOrder.path("id").asText()));

        JsonNode retryCheckout = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        assertEquals(
                "https://checkout.test/session/%s?amount=25.30&account=acct_seed_test"
                        .formatted(createdOrder.path("id").asText()),
                retryCheckout.path("checkoutUrl").asText());

        JsonNode reopenedOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("en_attente_paiement", reopenedOrder.path("status").asText());
        assertTrue(!containsOrder(getAuthorizedJson("/api/admin/orders", ownerJwt), createdOrder.path("id").asText()));

        postStripeWebhook(checkoutCompletedPayload(createdOrder.path("id").asText(), "pi_retry_success"));

        JsonNode paidOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("nouvelle", paidOrder.path("status").asText());
        JsonNode adminOrders = getAuthorizedJson("/api/admin/orders", ownerJwt);
        JsonNode visibleOrder = findOrder(adminOrders, createdOrder.path("id").asText());
        assertTrue(visibleOrder != null);
        assertEquals("nouvelle", visibleOrder.path("status").asText());
    }

    @Test
    void shouldApplyPaymentSettingsChangesToPublicCheckoutExploitability() throws Exception {
        restoreSeedDemoState();
        String ownerJwt = seedOwnerJwt();
        JsonNode createdOrder = createSeedDemoOrder();

        JsonNode clearedRestaurant = putAuthorizedJson("/api/admin/restaurant", ownerJwt, """
                {
                  "paymentProviderAccountId": "   "
                }
                """);
        assertTrue(clearedRestaurant.path("paymentProviderAccountId").isNull());

        JsonNode checkoutBlocked = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isBadRequest());
        assertEquals(
                "Ce restaurant n'a pas configuré les paiements en ligne",
                checkoutBlocked.path("message").asText());

        JsonNode restoredRestaurant = putAuthorizedJson("/api/admin/restaurant", ownerJwt, """
                {
                  "paymentProviderAccountId": "acct_reconfigured_test"
                }
                """);
        assertEquals("acct_reconfigured_test", restoredRestaurant.path("paymentProviderAccountId").asText());

        JsonNode checkout = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        assertEquals(
                "https://checkout.test/session/%s?amount=25.30&account=acct_reconfigured_test"
                        .formatted(createdOrder.path("id").asText()),
                checkout.path("checkoutUrl").asText());
    }
}
