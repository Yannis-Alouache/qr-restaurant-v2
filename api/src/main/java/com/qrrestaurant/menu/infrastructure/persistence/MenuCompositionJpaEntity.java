package com.qrrestaurant.menu.infrastructure.persistence;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "menu_composition")
public class MenuCompositionJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "restaurant_id", nullable = false)
    private UUID restaurantId;

    @Column(name = "composition_type", nullable = false)
    private String compositionType;

    @Column(name = "menu_item_id", nullable = false)
    private UUID menuItemId;

    @Column(name = "supplement_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal supplementPrice = BigDecimal.ZERO;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public String getCompositionType() { return compositionType; }
    public void setCompositionType(String compositionType) { this.compositionType = compositionType; }

    public UUID getMenuItemId() { return menuItemId; }
    public void setMenuItemId(UUID menuItemId) { this.menuItemId = menuItemId; }

    public BigDecimal getSupplementPrice() { return supplementPrice; }
    public void setSupplementPrice(BigDecimal supplementPrice) { this.supplementPrice = supplementPrice; }
}
