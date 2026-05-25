package com.qrrestaurant.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderStatusTest {

    @Test
    void shouldAllowTheExpectedLifecycleTransitions() {
        assertTrue(OrderStatus.en_attente_paiement.canTransitionTo(OrderStatus.nouvelle));
        assertTrue(OrderStatus.en_attente_paiement.canTransitionTo(OrderStatus.paiement_echoue));
        assertTrue(OrderStatus.nouvelle.canTransitionTo(OrderStatus.en_preparation));
        assertTrue(OrderStatus.en_preparation.canTransitionTo(OrderStatus.prete));
        assertTrue(OrderStatus.prete.canTransitionTo(OrderStatus.servie));
    }

    @Test
    void shouldRejectTransitionsOutsideTheStateMachine() {
        assertFalse(OrderStatus.nouvelle.canTransitionTo(OrderStatus.servie));
        assertFalse(OrderStatus.paiement_echoue.canTransitionTo(OrderStatus.servie));
        assertFalse(OrderStatus.servie.canTransitionTo(OrderStatus.prete));
    }
}
