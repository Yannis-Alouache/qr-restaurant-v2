package com.qrrestaurant.order.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void shouldAllowCheckoutWhenOrderIsWaitingForPayment() {
        Order order = new Order();

        assertDoesNotThrow(order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldRejectCheckoutWhenOrderIsAlreadyBeingPrepared() {
        Order order = new Order();
        order.setStatus(OrderStatus.en_preparation);

        assertThrows(Order.CheckoutUnavailableException.class, order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldAllowCheckoutWhenPreviousPaymentFailed() {
        Order order = new Order();
        order.setStatus(OrderStatus.paiement_echoue);

        assertDoesNotThrow(order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldStorePaymentTransactionAndMoveOrderToNouvelleWhenCheckoutCompletes() {
        Order order = new Order();

        order.markCheckoutCompleted("pi_123456");

        assertEquals(OrderStatus.nouvelle, order.getStatus());
        assertEquals("pi_123456", order.getPaymentTransactionId());
    }

    @Test
    void shouldMoveOrderToPaymentFailedWhenCheckoutExpires() {
        Order order = new Order();

        order.markCheckoutExpired();

        assertEquals(OrderStatus.paiement_echoue, order.getStatus());
    }

    @Test
    void shouldRejectCheckoutCompletionWhenOrderIsAlreadyBeingPrepared() {
        Order order = new Order();
        order.setStatus(OrderStatus.en_preparation);

        assertThrows(Order.InvalidStatusTransitionException.class,
                () -> order.markCheckoutCompleted("pi_123456"));
    }
}
