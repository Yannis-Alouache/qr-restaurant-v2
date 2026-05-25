package com.qrrestaurant.payment.domain;

import java.math.BigDecimal;

public interface PaymentGateway {

    String createCheckoutSession(String orderId, BigDecimal amount, String description,
                                  String destinationAccountId,
                                  String successUrl, String cancelUrl);

    class CheckoutSessionCreationException extends RuntimeException {
        public CheckoutSessionCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
