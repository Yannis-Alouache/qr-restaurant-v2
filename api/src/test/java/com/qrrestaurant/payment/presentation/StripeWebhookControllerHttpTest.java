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

    /**
     * Version d'API du compte Stripe, différente de celle épinglée par le SDK :
     * c'est la version des événements réels livrés par Stripe (dashboard ou CLI).
     */
    private static final String ACCOUNT_API_VERSION = "2025-04-30.basil";

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
    void shouldAcceptWebhookWhoseApiVersionDiffersFromSdkPinnedVersion() throws Exception {
        String orderId = createStandaloneOrder();

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(checkoutCompletedPayload(orderId, "pi_test_version_mismatch", ACCOUNT_API_VERSION)))
                        .content(checkoutCompletedPayload(orderId, "pi_test_version_mismatch", ACCOUNT_API_VERSION)))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/public/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("nouvelle"));
    }

    @Test
    void shouldTolerateDuplicateCheckoutCompletionDelivery() throws Exception {
        String orderId = createStandaloneOrder();
        String payload = checkoutCompletedPayload(orderId, "pi_test_duplicate", Stripe.API_VERSION);

        for (int i = 0; i < 2; i++) {
            mockMvc.perform(post("/api/webhooks/stripe")
                            .contentType(MediaType.APPLICATION_JSON)
                            .header("Stripe-Signature", stripeSignature(payload))
                            .content(payload))
                    .andExpect(status().isOk());
        }

        mockMvc.perform(get("/api/public/orders/" + orderId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("nouvelle"));
        org.junit.jupiter.api.Assertions.assertEquals(
                "pi_test_duplicate",
                orderRepository.findById(java.util.UUID.fromString(orderId)).orElseThrow().getPaymentTransactionId());
    }

    @Test
    void shouldAcknowledgeButIgnoreUnrelatedEventTypes() throws Exception {
        String payload = """
                {
                  "id": "evt_charge_succeeded",
                  "object": "event",
                  "api_version": "%s",
                  "type": "charge.succeeded",
                  "data": {
                    "object": {
                      "id": "ch_test",
                      "object": "charge"
                    }
                  }
                }
                """.formatted(ACCOUNT_API_VERSION);

        mockMvc.perform(post("/api/webhooks/stripe")
                        .contentType(MediaType.APPLICATION_JSON)
                        .header("Stripe-Signature", stripeSignature(payload))
                        .content(payload))
                .andExpect(status().isOk());
    }

    @Test
    void shouldAcceptAValidSignedCheckoutCompletionWebhook() throws Exception {
        String orderId = createStandaloneOrder();
        String payload = checkoutCompletedPayload(orderId, "pi_test_123", Stripe.API_VERSION);

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

    /**
     * Événement signé dont le JSON est lisible par le SDK (data.object est un
     * objet JSON) mais dont la session ne peut pas être désérialisée : le champ
     * metadata, attendu comme objet, est un nombre.
     */
    private String deserializationFailurePayload() {
        return """
                {
                  "id": "evt_unreadable_session",
                  "object": "event",
                  "api_version": "%s",
                  "type": "checkout.session.completed",
                  "data": {
                    "object": {
                      "id": "cs_unreadable_session",
                      "object": "checkout.session",
                      "metadata": 42
                    }
                  }
                }
                """.formatted(ACCOUNT_API_VERSION);
    }

    private String checkoutCompletedPayload(String orderId, String paymentIntentId, String apiVersion) {
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
                """.formatted(apiVersion, orderId, paymentIntentId, orderId);
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
