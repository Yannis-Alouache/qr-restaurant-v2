package com.qrrestaurant.payment.connect.domain;

import java.util.UUID;

/**
 * Port vers l'API Stripe Connect : crée le compte Express du restaurateur,
 * génère son lien d'onboarding et relit son état (KYC, encaissements).
 * L'implémentation déterministe sert aux tests, l'adaptateur Stripe SDK au runtime.
 */
public interface StripeConnectAccountGateway {

    /**
     * Crée un compte Connect Express pour le restaurant. Renvoie l'identifiant
     * Stripe (acct_...) à rattacher au restaurant.
     */
    String createExpressAccount(String ownerEmail, String restaurantName, UUID restaurantId);

    /**
     * Crée un lien d'onboarding (valide 24 h) vers le formulaire Stripe Express.
     * returnUrl : retour après une étape complétée ; refreshUrl : lien expiré ou
     * repris par le restaurateur.
     */
    String createOnboardingLink(String accountId, String returnUrl, String refreshUrl);

    /** Relit l'état courant du compte ; deleted=true si le compte n'existe plus chez Stripe. */
    StripeConnectAccountState retrieveAccount(String accountId);

    record StripeConnectAccountState(Boolean detailsSubmitted, Boolean payoutsEnabled, boolean deleted) {}

    /** Stripe est injoignable ou refuse la demande : l'action est à retenter plus tard. */
    class StripeConnectUnavailableException extends RuntimeException {
        public StripeConnectUnavailableException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
