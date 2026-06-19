package com.qrrestaurant.order.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class OrderItem {

    private final UUID id;
    private UUID orderId;
    private final UUID menuItemId;
    private final String name;
    private final int quantity;
    private BigDecimal unitPrice;
    private final UUID menuGroupId;
    private final String menuRole;

    private OrderItem(UUID id, UUID orderId, UUID menuItemId, String name,
                     int quantity, BigDecimal unitPrice, UUID menuGroupId, String menuRole) {
        this.id = id;
        this.orderId = orderId;
        this.menuItemId = menuItemId;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.menuGroupId = menuGroupId;
        this.menuRole = menuRole;
    }

    public static OrderItem create(UUID menuItemId, String name, int quantity,
                                   BigDecimal unitPrice, UUID menuGroupId, String menuRole) {
        Objects.requireNonNull(menuItemId, "menuItemId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(unitPrice, "unitPrice");
        return new OrderItem(UUID.randomUUID(), null, menuItemId, name, quantity, unitPrice, menuGroupId, menuRole);
    }

    public static OrderItem from(UUID id, UUID orderId, UUID menuItemId, String name,
                                 int quantity, BigDecimal unitPrice, UUID menuGroupId, String menuRole) {
        return new OrderItem(id, orderId, menuItemId, name, quantity, unitPrice, menuGroupId, menuRole);
    }

    public void assignToOrder(UUID orderId) {
        this.orderId = orderId;
    }

    public void reprice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public UUID getId() { return id; }
    public UUID getOrderId() { return orderId; }
    public UUID getMenuItemId() { return menuItemId; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public UUID getMenuGroupId() { return menuGroupId; }
    public String getMenuRole() { return menuRole; }
}
