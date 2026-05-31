package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderLifecycleSmokeTest extends AcceptanceTestBase {

    @Test
    void shouldKeepThePaidOrderTrackablePubliclyUntilServedWhileRemovingItFromAdminActiveBoard() throws Exception {
        restoreSeedDemoState();
        String ownerToken = seedOwnerBearerToken();

        JsonNode createdOrder = createSeedDemoOrder();
        postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        postStripeWebhook(checkoutCompletedPayload(createdOrder.path("id").asText(), "pi_served_flow"));

        String orderStatusPath = "/api/admin/orders/" + createdOrder.path("id").asText() + "/status";

        patchAuthorizedStatus(orderStatusPath, ownerToken, "en_preparation");
        JsonNode preparingOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("en_preparation", preparingOrder.path("status").asText());

        patchAuthorizedStatus(orderStatusPath, ownerToken, "prete");
        JsonNode readyOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("prete", readyOrder.path("status").asText());

        patchAuthorizedStatus(orderStatusPath, ownerToken, "servie");
        JsonNode servedOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("servie", servedOrder.path("status").asText());

        JsonNode adminOrders = getAuthorizedJson("/api/admin/orders", ownerToken);
        assertTrue(!containsOrder(adminOrders, createdOrder.path("id").asText()));
    }
}
