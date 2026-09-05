package com.qrrestaurant.payment.infrastructure.webhook;

import com.stripe.Stripe;
import com.stripe.StripeClient;
import com.stripe.exception.StripeException;
import com.stripe.model.WebhookEndpoint;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Garde-fou de mise en production : compare au démarrage la version d'API du
 * webhook endpoint configuré chez Stripe avec celle épinglée par le SDK Java.
 * Un écart est la cause classique de rejets silencieux d'événements (le
 * désérialiseur prudent du SDK refuse alors les payloads). Le contrôle n'alerte
 * jamais le fonctionnement : un simple WARN dans les logs au démarrage.
 *
 * Activé uniquement si STRIPE_WEBHOOK_ENDPOINT_ID est renseigné (identifiant
 * we_... de l'endpoint créé dans le dashboard Stripe). Sans valeur (développement
 * local avec le CLI « stripe listen »), le contrôle est désactivé.
 */
@Component
public class StripeWebhookVersionCheck {

    private static final Logger log = LoggerFactory.getLogger(StripeWebhookVersionCheck.class);

    private final StripeClient stripeClient;
    private final String webhookEndpointId;

    public StripeWebhookVersionCheck(@Value("${stripe.secret-key}") String secretKey,
                                     @Value("${stripe.webhook-endpoint-id:}") String webhookEndpointId) {
        this.stripeClient = new StripeClient(secretKey);
        this.webhookEndpointId = webhookEndpointId;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void verifyEndpointApiVersionMatchesSdk() {
        if (webhookEndpointId == null || webhookEndpointId.isBlank()) {
            log.info("Webhook Stripe : STRIPE_WEBHOOK_ENDPOINT_ID non renseigné, contrôle de version désactivé");
            return;
        }
        try {
            WebhookEndpoint endpoint = stripeClient.v1().webhookEndpoints().retrieve(webhookEndpointId);
            String endpointVersion = endpoint.getApiVersion();
            if (Stripe.API_VERSION.equals(endpointVersion)) {
                log.info("Webhook Stripe {} : version d'API {} alignée sur celle du SDK",
                        webhookEndpointId, endpointVersion);
            } else {
                log.warn("Webhook Stripe {} : version d'API {} différente de celle du SDK ({}). "
                                + "Mettre à jour la version de l'endpoint dans le dashboard Stripe "
                                + "pour rester sur le chemin de désérialisation vérifié.",
                        webhookEndpointId, endpointVersion, Stripe.API_VERSION);
            }
        } catch (StripeException e) {
            log.warn("Webhook Stripe : impossible de vérifier la version d'API de l'endpoint {} : {}",
                    webhookEndpointId, e.getMessage());
        }
    }
}
