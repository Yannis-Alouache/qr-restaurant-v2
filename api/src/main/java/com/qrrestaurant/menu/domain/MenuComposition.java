package com.qrrestaurant.menu.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class MenuComposition {

    public enum CompositionType {
        accompagnement, boisson
    }

    private UUID id;
    private UUID restaurantId;
    private CompositionType compositionType;
    private UUID menuItemId;
    private BigDecimal supplementPrice = BigDecimal.ZERO;

    public MenuComposition() {}

    public MenuComposition(UUID id, UUID restaurantId, CompositionType compositionType,
                           UUID menuItemId, BigDecimal supplementPrice) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.compositionType = compositionType;
        this.menuItemId = menuItemId;
        this.supplementPrice = supplementPrice;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public CompositionType getCompositionType() { return compositionType; }
    public void setCompositionType(CompositionType compositionType) { this.compositionType = compositionType; }

    public UUID getMenuItemId() { return menuItemId; }
    public void setMenuItemId(UUID menuItemId) { this.menuItemId = menuItemId; }

    public BigDecimal getSupplementPrice() { return supplementPrice; }
    public void setSupplementPrice(BigDecimal supplementPrice) { this.supplementPrice = supplementPrice; }
}
