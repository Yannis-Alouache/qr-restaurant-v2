package com.qrrestaurant.payment.infrastructure;

import com.qrrestaurant.payment.domain.PaymentGateway;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.checkout.Session;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.UUID;

@Service
public class StripePaymentGateway implements PaymentGateway {

    private final StripeClient stripeClient;

    public StripePaymentGateway(@Value("${stripe.secret-key}") String secretKey) {
        this.stripeClient = new StripeClient(secretKey);
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
            Session session = stripeClient.v1().checkout().sessions().create(params);
            return session.getUrl();
        } catch (StripeException e) {
            throw new PaymentGateway.CheckoutSessionCreationException(
                    "Le paiement en ligne est temporairement indisponible. Réessayez dans quelques instants.",
                    e);
        }
    }
}
