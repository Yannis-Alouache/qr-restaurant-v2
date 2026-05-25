package com.qrrestaurant.order.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class OrderItem {

    private UUID id;
    private UUID orderId;
    private UUID menuItemId;
    private String name;
    private int quantity;
    private BigDecimal unitPrice;
    private UUID menuGroupId;
    private String menuRole;

    public OrderItem() {}

    public OrderItem(UUID id, UUID orderId, UUID menuItemId, String name,
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

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getOrderId() { return orderId; }
    public void setOrderId(UUID orderId) { this.orderId = orderId; }

    public UUID getMenuItemId() { return menuItemId; }
    public void setMenuItemId(UUID menuItemId) { this.menuItemId = menuItemId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }

    public UUID getMenuGroupId() { return menuGroupId; }
    public void setMenuGroupId(UUID menuGroupId) { this.menuGroupId = menuGroupId; }

    public String getMenuRole() { return menuRole; }
    public void setMenuRole(String menuRole) { this.menuRole = menuRole; }
}
