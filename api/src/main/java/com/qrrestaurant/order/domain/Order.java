package com.qrrestaurant.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public class Order {

    private final UUID id;
    private final UUID restaurantId;
    private final UUID tableId;
    private OrderStatus status;
    private BigDecimal total;
    private String paymentTransactionId;
    private final Instant createdAt;

    private Order(UUID id, UUID restaurantId, UUID tableId, OrderStatus status,
                  BigDecimal total, String paymentTransactionId, Instant createdAt) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.tableId = tableId;
        this.status = status;
        this.total = total;
        this.paymentTransactionId = paymentTransactionId;
        this.createdAt = createdAt;
    }

    public static Order create(UUID restaurantId, UUID tableId, BigDecimal total) {
        Objects.requireNonNull(restaurantId, "restaurantId");
        Objects.requireNonNull(tableId, "tableId");
        Objects.requireNonNull(total, "total");
        return new Order(null, restaurantId, tableId, OrderStatus.en_attente_paiement,
                total, null, null);
    }

    public static Order from(UUID id, UUID restaurantId, UUID tableId, OrderStatus status,
                             BigDecimal total, String paymentTransactionId, Instant createdAt) {
        return new Order(id, restaurantId, tableId, status, total, paymentTransactionId, createdAt);
    }

    public void updateTotal(BigDecimal total) {
        this.total = total;
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

    /**
     * Confirme le paiement de la commande. Idempotent : Stripe peut livrer
     * plusieurs fois le même événement, une rediffusion ne change rien.
     *
     * @return true si la commande est passée en nouvelle, false si elle était
     *         déjà confirmée (livraison en double).
     */
    public boolean markCheckoutCompleted(String paymentTransactionId) {
        if (status != OrderStatus.en_attente_paiement) {
            return false;
        }
        this.paymentTransactionId = paymentTransactionId;
        transitionTo(OrderStatus.nouvelle);
        return true;
    }

    /**
     * Marque le paiement comme expiré. Idempotent : sans effet si la commande
     * a déjà été confirmée (expiration tardive) ou déjà marquée échouée.
     */
    public boolean markCheckoutExpired() {
        if (status != OrderStatus.en_attente_paiement) {
            return false;
        }
        transitionTo(OrderStatus.paiement_echoue);
        return true;
    }

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public UUID getTableId() { return tableId; }
    public OrderStatus getStatus() { return status; }
    public BigDecimal getTotal() { return total; }
    public String getPaymentTransactionId() { return paymentTransactionId; }
    public Instant getCreatedAt() { return createdAt; }

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
