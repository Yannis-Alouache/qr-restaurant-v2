package com.qrrestaurant.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderTest {

    @Test
    void shouldAllowCheckoutWhenOrderIsWaitingForPayment() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        assertDoesNotThrow(order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldRejectCheckoutWhenOrderIsAlreadyBeingPrepared() {
        Order order = orderWithStatus(OrderStatus.en_preparation);

        assertThrows(Order.CheckoutUnavailableException.class, order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldAllowCheckoutWhenPreviousPaymentFailed() {
        Order order = orderWithStatus(OrderStatus.paiement_echoue);

        assertDoesNotThrow(order::assertCanCreateCheckoutSession);
    }

    @Test
    void shouldStorePaymentTransactionAndMoveOrderToNouvelleWhenCheckoutCompletes() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        order.markCheckoutCompleted("pi_123456");

        assertEquals(OrderStatus.nouvelle, order.getStatus());
        assertEquals("pi_123456", order.getPaymentTransactionId());
    }

    @Test
    void shouldMoveOrderToPaymentFailedWhenCheckoutExpires() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        order.markCheckoutExpired();

        assertEquals(OrderStatus.paiement_echoue, order.getStatus());
    }

    @Test
    void shouldRejectCheckoutCompletionWhenOrderIsAlreadyBeingPrepared() {
        Order order = orderWithStatus(OrderStatus.en_preparation);

        assertThrows(Order.InvalidStatusTransitionException.class,
                () -> order.markCheckoutCompleted("pi_123456"));
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.from(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status,
                BigDecimal.ZERO, null, Instant.now());
    }
}
