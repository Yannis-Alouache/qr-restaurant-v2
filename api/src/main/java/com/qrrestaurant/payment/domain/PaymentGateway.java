package com.qrrestaurant.payment.domain;

import java.math.BigDecimal;

public interface PaymentGateway {

    String createCheckoutSession(String orderId, BigDecimal amount, String description,
                                  String destinationAccountId,
                                  String successUrl, String cancelUrl);

    /**
     * Rembourse intégralement le paiement associé (et annule le transfert
     * Connect correspondant).
     */
    void refundPayment(String paymentIntentId);

    class CheckoutSessionCreationException extends RuntimeException {
        public CheckoutSessionCreationException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class RefundException extends RuntimeException {
        public RefundException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
