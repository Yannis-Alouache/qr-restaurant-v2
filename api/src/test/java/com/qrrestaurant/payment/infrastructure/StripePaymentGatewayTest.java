package com.qrrestaurant.payment.infrastructure;
import com.qrrestaurant.payment.infrastructure.gateway.StripeCheckoutSessionClient;
import com.qrrestaurant.payment.infrastructure.gateway.StripePaymentGateway;

import com.stripe.param.checkout.SessionCreateParams;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;

class StripePaymentGatewayTest {

    @Test
    void shouldBuildAStripeCheckoutSessionWithMetadataTransferAndHostedCheckoutUrls() {
        RecordingStripeCheckoutSessionClient checkoutSessionClient = new RecordingStripeCheckoutSessionClient();
        StripePaymentGateway gateway = new StripePaymentGateway(checkoutSessionClient);

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

    private static final class RecordingStripeCheckoutSessionClient implements StripeCheckoutSessionClient {

        private SessionCreateParams params;

        @Override
        public String createCheckoutSessionUrl(SessionCreateParams params) {
            this.params = params;
            return "https://checkout.stripe.test/session/" + params.getMetadata().get("order_id");
        }
    }
}
