package com.qrrestaurant.payment.presentation;

import com.qrrestaurant.payment.application.HandleWebhookUseCase;
import com.stripe.model.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/webhooks/stripe")
public class StripeWebhookController {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookController.class);

    private final StripeWebhookPayloadParser payloadParser;
    private final HandleWebhookUseCase handleWebhookUseCase;

    public StripeWebhookController(StripeWebhookPayloadParser payloadParser,
                                   HandleWebhookUseCase handleWebhookUseCase) {
        this.payloadParser = payloadParser;
        this.handleWebhookUseCase = handleWebhookUseCase;
    }

    @PostMapping
    public ResponseEntity<Void> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {
        Event event = payloadParser.verify(payload, sigHeader);

        // Les événements hors cycle de commande sont acquittés sans traitement :
        // une réponse non 2xx déclencherait des tentatives de livraison inutiles.
        if (!payloadParser.isHandled(event.getType())) {
            log.info("Ignoring Stripe webhook event type {}", event.getType());
            return ResponseEntity.ok().build();
        }

        StripeWebhookPayloadParser.ParsedWebhook webhook = payloadParser.extractCheckoutSession(event);
        if (StripeWebhookPayloadParser.CHECKOUT_COMPLETED_EVENT.equals(event.getType())) {
            handleWebhookUseCase.handleCheckoutCompleted(webhook.orderId(), webhook.paymentIntentId());
        } else {
            handleWebhookUseCase.handleCheckoutExpired(webhook.orderId());
        }
        return ResponseEntity.ok().build();
    }
}
