package com.qrrestaurant.order.domain;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
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
    void shouldStorePaymentTransactionAndCustomerEmailWhenCheckoutCompletes() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        order.markCheckoutCompleted("pi_123456", "client@example.com");

        assertEquals(OrderStatus.nouvelle, order.getStatus());
        assertEquals("pi_123456", order.getPaymentTransactionId());
        assertEquals("client@example.com", order.getCustomerEmail());
    }

    @Test
    void shouldAcceptCheckoutCompletionWithoutCustomerEmail() {
        // Webhook atypique sans customer_details : le paiement reste confirmé.
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        order.markCheckoutCompleted("pi_123456", null);

        assertEquals(OrderStatus.nouvelle, order.getStatus());
        assertNull(order.getCustomerEmail());
    }

    @Test
    void shouldMoveOrderToPaymentFailedWhenCheckoutExpires() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);

        order.markCheckoutExpired();

        assertEquals(OrderStatus.paiement_echoue, order.getStatus());
    }

    @Test
    void shouldIgnoreDuplicateCheckoutCompletionWhenOrderIsAlreadyBeingPrepared() {
        Order order = orderWithStatus(OrderStatus.en_preparation);

        boolean changed = order.markCheckoutCompleted("pi_123456", "client@example.com");

        assertFalse(changed);
        assertEquals(OrderStatus.en_preparation, order.getStatus());
        assertNull(order.getPaymentTransactionId());
        assertNull(order.getCustomerEmail());
    }

    @Test
    void shouldBeIdempotentWhenCheckoutCompletionIsDeliveredTwice() {
        Order order = orderWithStatus(OrderStatus.en_attente_paiement);
        order.markCheckoutCompleted("pi_first", "first@example.com");

        boolean replayChanged = order.markCheckoutCompleted("pi_second", "second@example.com");

        assertFalse(replayChanged);
        assertEquals(OrderStatus.nouvelle, order.getStatus());
        assertEquals("pi_first", order.getPaymentTransactionId());
        assertEquals("first@example.com", order.getCustomerEmail());
    }

    private Order orderWithStatus(OrderStatus status) {
        return Order.from(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), status,
                BigDecimal.ZERO, null, null, Instant.now());
    }
}
