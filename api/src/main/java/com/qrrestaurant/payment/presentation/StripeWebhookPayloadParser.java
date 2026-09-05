package com.qrrestaurant.payment.presentation;

import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.EventDataObjectDeserializer;
import com.stripe.model.StripeObject;
import com.stripe.model.checkout.Session;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Adaptateur Stripe du webhook : vérifie la signature d'un événement puis en
 * extrait les champs exploités par l'application, quelle que soit la version
 * d'API dans laquelle Stripe l'a rendu (compte, CLI ou dashboard peuvent épingler
 * des versions différentes de celle du SDK Java).
 */
@Component
public class StripeWebhookPayloadParser {

    public static final String CHECKOUT_COMPLETED_EVENT = "checkout.session.completed";
    public static final String CHECKOUT_EXPIRED_EVENT = "checkout.session.expired";

    private final StripeClient stripeClient;
    private final String webhookSecret;

    public StripeWebhookPayloadParser(@Value("${stripe.secret-key}") String secretKey,
                                      @Value("${stripe.webhook-secret:}") String webhookSecret) {
        this.stripeClient = new StripeClient(secretKey);
        this.webhookSecret = webhookSecret;
    }

    public record ParsedWebhook(String orderId, String paymentIntentId) {}

    public boolean isHandled(String eventType) {
        return CHECKOUT_COMPLETED_EVENT.equals(eventType) || CHECKOUT_EXPIRED_EVENT.equals(eventType);
    }

    public Event verify(String payload, String signatureHeader) {
        try {
            return stripeClient.constructEvent(payload, signatureHeader, webhookSecret);
        } catch (Exception e) {
            throw new InvalidWebhookSignatureException();
        }
    }

    public ParsedWebhook extractCheckoutSession(Event event) {
        Session session = deserializeSession(event.getDataObjectDeserializer());
        String orderId = session.getMetadata() == null ? null : session.getMetadata().get("order_id");
        if (orderId == null || orderId.isBlank()) {
            throw new MissingOrderMetadataException();
        }
        return new ParsedWebhook(orderId, session.getPaymentIntent());
    }

    // getObject() refuse les événements rendus dans une autre version d'API que
    // celle épinglée par le SDK : deserializeUnsafe() sert de filet de sécurité,
    // les champs lus (metadata.order_id, payment_intent) étant stables entre versions.
    private Session deserializeSession(EventDataObjectDeserializer deserializer) {
        Object rawObject;
        try {
            Optional<StripeObject> safeObject = deserializer.getObject();
            rawObject = safeObject.isPresent() ? safeObject.get() : deserializer.deserializeUnsafe();
        } catch (StripeException | RuntimeException e) {
            throw new InvalidWebhookPayloadException();
        }
        if (!(rawObject instanceof Session session)) {
            throw new InvalidWebhookPayloadException();
        }
        return session;
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
