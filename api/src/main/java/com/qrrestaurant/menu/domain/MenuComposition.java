package com.qrrestaurant.menu.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class MenuComposition {

    public enum CompositionType {
        accompagnement, boisson
    }

    private final UUID id;
    private final UUID restaurantId;
    private final CompositionType compositionType;
    private final UUID menuItemId;
    private final BigDecimal supplementPrice;

    private MenuComposition(UUID id, UUID restaurantId, CompositionType compositionType,
                            UUID menuItemId, BigDecimal supplementPrice) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.compositionType = compositionType;
        this.menuItemId = menuItemId;
        this.supplementPrice = supplementPrice;
    }

    public static MenuComposition create(UUID restaurantId, CompositionType compositionType,
                                         UUID menuItemId, BigDecimal supplementPrice) {
        Objects.requireNonNull(restaurantId, "restaurantId");
        Objects.requireNonNull(compositionType, "compositionType");
        Objects.requireNonNull(menuItemId, "menuItemId");
        BigDecimal price = supplementPrice != null ? supplementPrice : BigDecimal.ZERO;
        return new MenuComposition(null, restaurantId, compositionType, menuItemId, price);
    }

    public static MenuComposition from(UUID id, UUID restaurantId, CompositionType compositionType,
                                       UUID menuItemId, BigDecimal supplementPrice) {
        return new MenuComposition(id, restaurantId, compositionType, menuItemId, supplementPrice);
    }

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public CompositionType getCompositionType() { return compositionType; }
    public UUID getMenuItemId() { return menuItemId; }
    public BigDecimal getSupplementPrice() { return supplementPrice; }
}
