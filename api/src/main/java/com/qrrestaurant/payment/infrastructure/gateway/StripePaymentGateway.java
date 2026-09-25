package com.qrrestaurant.payment.infrastructure.gateway;

import com.qrrestaurant.payment.domain.PaymentGateway;
import com.stripe.exception.StripeException;
import com.stripe.param.checkout.SessionCreateParams;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;

@Service
public class StripePaymentGateway implements PaymentGateway {

    private final StripeCheckoutSessionClient checkoutSessionClient;
    private final StripeRefundClient refundClient;

    public StripePaymentGateway(StripeCheckoutSessionClient checkoutSessionClient,
                                StripeRefundClient refundClient) {
        this.checkoutSessionClient = checkoutSessionClient;
        this.refundClient = refundClient;
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
            // Idempotency-Key par fenêtre d'une seconde : un double-clic (ou un
            // retry réseau) dans la même seconde renvoie la même session au lieu
            // d'en créer deux ; un nouvel essai quelques secondes plus tard
            // repart d'une session fraîche (l'ancienne peut être expirée).
            String idempotencyKey = "checkout-" + orderId + "-" + Instant.now().getEpochSecond();
            return checkoutSessionClient.createCheckoutSessionUrl(params, idempotencyKey);
        } catch (StripeException e) {
            throw new PaymentGateway.CheckoutSessionCreationException(
                    "Le paiement en ligne est temporairement indisponible. Réessayez dans quelques instants.",
                    e);
        }
    }

    @Override
    public void refundPayment(String paymentIntentId) {
        try {
            // Clé déterministe par paiement : un retry (panne réseau après un
            // remboursement créé) renvoie le même refund Stripe au lieu d'en
            // créer un second — impossible de rembourser deux fois par accident.
            refundClient.refundPaymentIntent(paymentIntentId, "refund-" + paymentIntentId);
        } catch (StripeException e) {
            throw new PaymentGateway.RefundException(
                    "Le remboursement est temporairement indisponible. Réessayez dans quelques instants.",
                    e);
        }
    }
}
