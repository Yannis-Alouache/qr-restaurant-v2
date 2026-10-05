package com.qrrestaurant.order.domain;

/**
 * Port d'envoi du reçu de commande. Best effort par contrat : une panne
 * d'envoi ne doit jamais invalider la confirmation de paiement déjà enregistrée.
 */
public interface OrderConfirmationMailer {

    void send(OrderConfirmationEmail email);
}
