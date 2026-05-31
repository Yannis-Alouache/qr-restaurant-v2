package com.qrrestaurant.payment.infrastructure.gateway;

import com.qrrestaurant.payment.domain.PaymentGateway;

import java.math.BigDecimal;

public class DeterministicPaymentGateway implements PaymentGateway {
    @Override
    public String createCheckoutSession(String orderId, BigDecimal amount, String description,
                                        String destinationAccountId, String successUrl, String cancelUrl) {
        return "https://checkout.test/session/%s?amount=%s&account=%s"
                .formatted(orderId, amount.toPlainString(), destinationAccountId);
    }
}
