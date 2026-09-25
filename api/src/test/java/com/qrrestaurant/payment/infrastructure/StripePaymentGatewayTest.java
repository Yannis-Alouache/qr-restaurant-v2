package com.qrrestaurant.payment.infrastructure.gateway;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.payment.infrastructure.gateway.StripeCheckoutSessionClient;
import com.qrrestaurant.payment.infrastructure.gateway.StripePaymentGateway;
import com.qrrestaurant.payment.infrastructure.gateway.StripeRefundClient;

import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StripePaymentGatewayTest {

    @Test
    void shouldBuildAStripeCheckoutSessionWithMetadataTransferAndHostedCheckoutUrls() {
        RecordingStripeCheckoutSessionClient checkoutSessionClient = new RecordingStripeCheckoutSessionClient();
        StripePaymentGateway gateway = new StripePaymentGateway(checkoutSessionClient, new RecordingStripeRefundClient());

        String checkoutUrl = gateway.createCheckoutSession(
                "order-123",
                new BigDecimal("13.50"),
                "Commande #order-123",
                "acct_restaurant_123",
                "https://client.example/order/order-123/confirmation",
                "https://client.example/order/order-123/cancelled"
        );

        SessionCreateParams params = checkoutSessionClient.params;

        assertEquals("https://checkout.stripe.test/session/order-123", checkoutUrl);
        assertEquals(SessionCreateParams.Mode.PAYMENT, params.getMode());
        assertEquals("https://client.example/order/order-123/confirmation", params.getSuccessUrl());
        assertEquals("https://client.example/order/order-123/cancelled", params.getCancelUrl());
        assertEquals("order-123", params.getMetadata().get("order_id"));
        assertEquals("Commande #order-123", params.getPaymentIntentData().getDescription());
        assertEquals("acct_restaurant_123", params.getPaymentIntentData().getTransferData().getDestination());
        assertEquals(1L, params.getLineItems().getFirst().getQuantity());
        assertEquals("eur", params.getLineItems().getFirst().getPriceData().getCurrency());
        assertEquals(1350L, params.getLineItems().getFirst().getPriceData().getUnitAmount());
        assertEquals("Commande #order-123", params.getLineItems().getFirst().getPriceData().getProductData().getName());
    }

    @Test
    void shouldSendAnIdempotencyKeyScopedToTheOrderAndCurrentSecond() {
        RecordingStripeCheckoutSessionClient checkoutSessionClient = new RecordingStripeCheckoutSessionClient();
        StripePaymentGateway gateway = new StripePaymentGateway(checkoutSessionClient, new RecordingStripeRefundClient());

        gateway.createCheckoutSession(
                "order-123", new BigDecimal("13.50"), "Commande #order-123", "acct_x",
                "https://client.example/success", "https://client.example/cancel");

        assertTrue(checkoutSessionClient.idempotencyKey.startsWith("checkout-order-123-"),
                "clé d'idempotence liée à la commande : " + checkoutSessionClient.idempotencyKey);
    }

    @Test
    void shouldRefundWithADeterministicIdempotencyKeyDerivedFromThePayment() {
        RecordingStripeRefundClient refundClient = new RecordingStripeRefundClient();
        StripePaymentGateway gateway = new StripePaymentGateway(
                new RecordingStripeCheckoutSessionClient(), refundClient);

        gateway.refundPayment("pi_123");
        gateway.refundPayment("pi_123");

        assertEquals(2, refundClient.calls);
        assertEquals("refund-pi_123", refundClient.lastIdempotencyKey);
        assertEquals("pi_123", refundClient.lastPaymentIntentId);
    }

    @Test
    void shouldExposeExplicitErrorWhenStripeRefundFails() {
        StripeRefundClient failing = (paymentIntentId, idempotencyKey) -> {
            throw new com.stripe.exception.ApiConnectionException("network down", null);
        };
        StripePaymentGateway gateway = new StripePaymentGateway(new RecordingStripeCheckoutSessionClient(), failing);

        PaymentGateway.RefundException thrown = assertThrows(PaymentGateway.RefundException.class,
                () -> gateway.refundPayment("pi_123"));
        assertTrue(thrown.getMessage().contains("remboursement est temporairement indisponible"));
    }

    private static final class RecordingStripeCheckoutSessionClient implements StripeCheckoutSessionClient {

        private SessionCreateParams params;
        private String idempotencyKey;

        @Override
        public String createCheckoutSessionUrl(SessionCreateParams params, String idempotencyKey) {
            this.params = params;
            this.idempotencyKey = idempotencyKey;
            return "https://checkout.stripe.test/session/" + params.getMetadata().get("order_id");
        }
    }

    private static final class RecordingStripeRefundClient implements StripeRefundClient {

        private int calls;
        private String lastPaymentIntentId;
        private String lastIdempotencyKey;

        @Override
        public void refundPaymentIntent(String paymentIntentId, String idempotencyKey) {
            this.calls++;
            this.lastPaymentIntentId = paymentIntentId;
            this.lastIdempotencyKey = idempotencyKey;
        }
    }
}
