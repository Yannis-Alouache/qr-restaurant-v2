package com.qrrestaurant.payment.infrastructure.gateway;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.net.RequestOptions;
import com.stripe.param.RefundCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class StripeSdkRefundClient implements StripeRefundClient {

    private final StripeClient stripeClient;

    public StripeSdkRefundClient(@Value("${stripe.secret-key}") String secretKey) {
        this.stripeClient = new StripeClient(secretKey);
    }

    @Override
    public void refundPaymentIntent(String paymentIntentId, String idempotencyKey) throws StripeException {
        stripeClient.v1().refunds().create(
                RefundCreateParams.builder()
                        .setPaymentIntent(paymentIntentId)
                        // Destination charge Connect : le remboursement côté
                        // plateforme annule aussi le transfert vers le compte
                        // du restaurateur.
                        .setReverseTransfer(true)
                        .build(),
                RequestOptions.builder().setIdempotencyKey(idempotencyKey).build());
    }
}
