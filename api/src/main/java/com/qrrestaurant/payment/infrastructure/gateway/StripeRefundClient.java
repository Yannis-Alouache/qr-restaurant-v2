package com.qrrestaurant.payment.infrastructure.gateway;

import com.stripe.exception.StripeException;

public interface StripeRefundClient {

    void refundPaymentIntent(String paymentIntentId, String idempotencyKey) throws StripeException;
}
