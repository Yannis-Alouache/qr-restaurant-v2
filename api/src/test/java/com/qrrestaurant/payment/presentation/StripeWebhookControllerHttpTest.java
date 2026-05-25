package com.qrrestaurant.payment.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrrestaurant.order.domain.OrderRepository;
import com.stripe.Stripe;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StripeWebhookControllerHttpTest extends AbstractPostgresIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private OrderRepository orderRepository;

    private String validSignature;

    @BeforeEach
    void setUp() throws Exception {
        validSignature = stripeSignature(deserializationFailurePayload());
    }

    @Test
    void shouldRejectWebhookWhenStripeSignatureIsMissing() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signature Stripe manquante"));
    }

    @Test
    void shouldRejectWebhookWhenSignatureIsInvalid() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", "invalid")
                        .content("{\"type\":\"checkout.session.completed\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Signature Stripe invalide"));
    }

    @Test
    void shouldRejectWebhookWhenPayloadCannotBeDeserialized() throws Exception {
        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", validSignature)
                        .content(deserializationFailurePayload()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Payload Stripe invalide"));
    }

    @Test
    void shouldAcceptAValidSignedCheckoutCompletionWebhook() throws Exception {
        String orderId = createStandaloneOrder();
        String payload = checkoutCompletedPayload(orderId, "pi_test_123");

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("nouvelle"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "pi_test_123",
                orderRepository.findById(java.util.UUID.fromString(orderId)).orElseThrow().getPaymentTransactionId());
    }

    private String deserializationFailurePayload() {
        return """
                {
                  "id": "evt_missing_api_version",
                  "object": "event",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_missing_api_version",
                      "object": "checkout.session",
                      "metadata": {
                        "order_id": "b0eebc99-9c0b-4ef8-bb6d-6bb9bd380a22"
                      }
                    }
                  }
                }
                """;
    }

    private String checkoutCompletedPayload(String orderId, String paymentIntentId) {
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

    private String createStandaloneOrder() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "naia-burger",
                                  "tableId": "%s",
                                  "items": [
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(TABLE_1_ID, BROWNIE_ID)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.path("id").asText();
    }

    private String stripeSignature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signedPayload = timestamp + "." + payload;
        String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=%d,v1=%s".formatted(timestamp, signature);
    }
}
