package com.qrrestaurant.payment.infrastructure;

import com.qrrestaurant.payment.domain.PaymentGateway;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
public class StripePaymentGateway implements PaymentGateway {

    private final StripeCheckoutSessionClient checkoutSessionClient;

    public StripePaymentGateway(StripeCheckoutSessionClient checkoutSessionClient) {
        this.checkoutSessionClient = checkoutSessionClient;
    }

    @Override
    public String createCheckoutSession(String orderId, BigDecimal amount, String description,
                                         String destinationAccountId,
                                         String successUrl, String cancelUrl) {
        long amountCents = amount.multiply(BigDecimal.valueOf(100)).longValue();

        SessionCreateParams params = SessionCreateParams.builder()
                .setMode(SessionCreateParams.Mode.PAYMENT)
                .setSuccessUrl(successUrl)
                .setCancelUrl(cancelUrl)
                .putMetadata("order_id", orderId)
                .setPaymentIntentData(SessionCreateParams.PaymentIntentData.builder()
                        .setDescription(description)
                        .setTransferData(SessionCreateParams.PaymentIntentData.TransferData.builder()
                                .setDestination(destinationAccountId)
                                .build())
                        .build())
                .addLineItem(SessionCreateParams.LineItem.builder()
                        .setQuantity(1L)
                        .setPriceData(SessionCreateParams.LineItem.PriceData.builder()
                                .setCurrency("eur")
                                .setUnitAmount(amountCents)
                                .setProductData(SessionCreateParams.LineItem.PriceData.ProductData.builder()
                                        .setName(description)
                                        .build())
                                .build())
                        .build())
                .build();
        try {
            return checkoutSessionClient.createCheckoutSessionUrl(params);
        } catch (StripeException e) {
            throw new PaymentGateway.CheckoutSessionCreationException(
                    "Le paiement en ligne est temporairement indisponible. Réessayez dans quelques instants.",
                    e);
        }
    }
}
