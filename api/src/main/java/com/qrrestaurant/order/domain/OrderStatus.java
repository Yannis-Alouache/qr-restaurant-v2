package com.qrrestaurant.order.domain;

public enum OrderStatus {
    en_attente_paiement,
    paiement_echoue,
    nouvelle,
    en_preparation,
    prete,
    servie;

    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case en_attente_paiement -> target == paiement_echoue || target == nouvelle;
            case paiement_echoue -> target == en_attente_paiement;
            case nouvelle -> target == en_preparation;
            case en_preparation -> target == prete;
            case prete -> target == servie;
            case servie -> false;
        };
    }
}
