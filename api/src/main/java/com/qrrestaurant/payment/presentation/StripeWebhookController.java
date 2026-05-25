package com.qrrestaurant.payment.presentation;

import com.qrrestaurant.payment.application.HandleWebhookUseCase;
import com.stripe.StripeClient;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.checkout.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeClient stripeClient;
    private final String webhookSecret;
    private final HandleWebhookUseCase handleWebhookUseCase;

    public StripeWebhookController(@Value("${stripe.secret-key}") String secretKey,
                                    @Value("${stripe.webhook-secret:whsec_placeholder}") String webhookSecret,
                                    HandleWebhookUseCase handleWebhookUseCase) {
        this.stripeClient = new StripeClient(secretKey);
        this.webhookSecret = webhookSecret;
        this.handleWebhookUseCase = handleWebhookUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        Event event;
        try {
            event = stripeClient.constructEvent(payload, sigHeader, webhookSecret);
        } catch (Exception e) {
            throw new InvalidWebhookSignatureException();
        }

        Session session = deserializeSession(event.getDataObjectDeserializer());
        if (session.getMetadata() == null) {
            throw new MissingOrderMetadataException();
        }
        String orderId = session.getMetadata().get("order_id");
        if (orderId == null || orderId.isBlank()) {
            throw new MissingOrderMetadataException();
        }

        if ("checkout.session.completed".equals(event.getType())) {
            handleWebhookUseCase.handleCheckoutCompleted(orderId, session.getPaymentIntent());
            return ResponseEntity.ok().build();
        }

        if ("checkout.session.expired".equals(event.getType())) {
            handleWebhookUseCase.handleCheckoutExpired(orderId);
            return ResponseEntity.ok().build();
        }

        log.info("Ignoring Stripe webhook event type {}", event.getType());
        return ResponseEntity.ok().build();
    }

    private Session deserializeSession(EventDataObjectDeserializer deserializer) {
        try {
            Object rawObject = deserializer.getObject().orElseThrow(InvalidWebhookPayloadException::new);
            if (!(rawObject instanceof Session session)) {
                throw new InvalidWebhookPayloadException();
            }
            return session;
        } catch (NullPointerException exception) {
            throw new InvalidWebhookPayloadException();
        }
    }

    public static class InvalidWebhookSignatureException extends IllegalArgumentException {
        public InvalidWebhookSignatureException() {
            super("Signature Stripe invalide");
        }
    }

    public static class MissingOrderMetadataException extends IllegalArgumentException {
        public MissingOrderMetadataException() {
            super("Identifiant de commande manquant dans le webhook");
        }
    }

    public static class InvalidWebhookPayloadException extends IllegalArgumentException {
        public InvalidWebhookPayloadException() {
            super("Payload Stripe invalide");
        }
    }
}
