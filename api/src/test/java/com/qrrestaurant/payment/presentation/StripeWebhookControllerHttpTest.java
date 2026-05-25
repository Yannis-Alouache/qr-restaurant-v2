package com.qrrestaurant.payment.presentation;

import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StripeWebhookControllerHttpTest extends AbstractPostgresIntegrationTest {

    private static final String WEBHOOK_SECRET = "whsec_test";

    @Autowired
    private MockMvc mockMvc;

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

    private String stripeSignature(String payload) throws Exception {
        long timestamp = Instant.now().getEpochSecond();
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(WEBHOOK_SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        String signedPayload = timestamp + "." + payload;
        String signature = HexFormat.of().formatHex(mac.doFinal(signedPayload.getBytes(StandardCharsets.UTF_8)));
        return "t=%d,v1=%s".formatted(timestamp, signature);
    }
}
