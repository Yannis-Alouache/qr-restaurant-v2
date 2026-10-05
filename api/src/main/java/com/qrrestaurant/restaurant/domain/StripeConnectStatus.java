package com.qrrestaurant.restaurant.domain;

import java.util.Locale;

/**
 * État d'onboarding du compte Stripe Connect (Express) d'un restaurateur :
 *   PENDING    compte Stripe créé, inscription (KYC) non terminée ;
 *   ACTIVE     inscription terminée, le restaurant peut encaisser ;
 *   RESTRICTED inscription terminée mais encaissement ou versements bloqués
 *              (documents manquants : action requise du restaurateur).
 * L'absence de statut (null côté persistance) signifie : aucun compte connecté.
 */
public enum StripeConnectStatus {
    PENDING,
    ACTIVE,
    RESTRICTED;

    /**
     * Traduit l'état brut du compte Stripe renvoyé par l'API en statut métier.
     * detailsSubmitted manquant (compte créé il y a moins d'une seconde, champs
     * non encore calculés par Stripe) est traité comme un onboarding en cours.
     */
    public static StripeConnectStatus fromStripeAccountState(Boolean detailsSubmitted, Boolean payoutsEnabled) {
        if (!Boolean.TRUE.equals(detailsSubmitted)) {
            return PENDING;
        }
        return Boolean.TRUE.equals(payoutsEnabled) ? ACTIVE : RESTRICTED;
    }

    /** Valeur stockée en base (ex. "active"). */
    public String value() {
        return name().toLowerCase(Locale.ROOT);
    }

    /** Lit la valeur stockée en base ; null/blanc = aucun compte connecté. */
    public static StripeConnectStatus fromStored(String stored) {
        if (stored == null || stored.isBlank()) {
            return null;
        }
        return StripeConnectStatus.valueOf(stored.trim().toUpperCase(Locale.ROOT));
    }
}
