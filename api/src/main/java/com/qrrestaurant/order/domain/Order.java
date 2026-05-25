package com.qrrestaurant.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public class Order {

    private UUID id;
    private UUID restaurantId;
    private UUID tableId;
    private OrderStatus status = OrderStatus.en_attente_paiement;
    private BigDecimal total;
    private String paymentTransactionId;
    private Instant createdAt;

    public Order() {}

    public Order(UUID id, UUID restaurantId, UUID tableId, OrderStatus status,
                 BigDecimal total, String paymentTransactionId, Instant createdAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.tableId = tableId;
        this.status = status;
        this.total = total;
        this.paymentTransactionId = paymentTransactionId;
        this.createdAt = createdAt;
    }

    public void transitionTo(OrderStatus target) {
        if (!status.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(status, target);
        }
        this.status = target;
    }

    public void advanceForAdministration(OrderStatus target) {
        if (status == OrderStatus.en_attente_paiement || status == OrderStatus.paiement_echoue) {
            throw new UnpaidOrderStatusUpdateException();
        }
        transitionTo(target);
    }

    public void assertCanCreateCheckoutSession() {
        if (status != OrderStatus.en_attente_paiement && status != OrderStatus.paiement_echoue) {
            throw new CheckoutUnavailableException(status);
        }
    }

    public void markCheckoutSessionCreated() {
        if (status == OrderStatus.paiement_echoue) {
            transitionTo(OrderStatus.en_attente_paiement);
        }
    }

    public void markCheckoutCompleted(String paymentTransactionId) {
        this.paymentTransactionId = paymentTransactionId;
        transitionTo(OrderStatus.nouvelle);
    }

    public void markCheckoutExpired() {
        transitionTo(OrderStatus.paiement_echoue);
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public UUID getTableId() { return tableId; }
    public void setTableId(UUID tableId) { this.tableId = tableId; }

    public OrderStatus getStatus() { return status; }
    public void setStatus(OrderStatus status) { this.status = status; }

    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }

    public String getPaymentTransactionId() { return paymentTransactionId; }
    public void setPaymentTransactionId(String paymentTransactionId) { this.paymentTransactionId = paymentTransactionId; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public static class InvalidStatusTransitionException extends RuntimeException {
        public InvalidStatusTransitionException(OrderStatus from, OrderStatus to) {
            super("Transition invalide: " + from + " → " + to);
        }
    }

    public static class CheckoutUnavailableException extends RuntimeException {
        public CheckoutUnavailableException(OrderStatus status) {
            super("Commande déjà traitée: " + status);
        }
    }

    public static class UnpaidOrderStatusUpdateException extends RuntimeException {
        public UnpaidOrderStatusUpdateException() {
            super("Paiement non confirmé pour cette commande");
        }
    }
}
