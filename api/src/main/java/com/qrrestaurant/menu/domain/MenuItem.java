package com.qrrestaurant.menu.domain;

import java.math.BigDecimal;
import java.util.UUID;

public class MenuItem {

    private UUID id;
    private UUID categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imagePath;
    private boolean available = true;
    private UUID menuVariantOf;

    public MenuItem() {}

    public MenuItem(UUID id, UUID categoryId, String name, String description,
                    BigDecimal price, String imagePath, boolean available, UUID menuVariantOf) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.available = available;
        this.menuVariantOf = menuVariantOf;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getCategoryId() { return categoryId; }
    public void setCategoryId(UUID categoryId) { this.categoryId = categoryId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public BigDecimal getPrice() { return price; }
    public void setPrice(BigDecimal price) { this.price = price; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public boolean isAvailable() { return available; }
    public void setAvailable(boolean available) { this.available = available; }

    public UUID getMenuVariantOf() { return menuVariantOf; }
    public void setMenuVariantOf(UUID menuVariantOf) { this.menuVariantOf = menuVariantOf; }

    public boolean isMenuVariant() { return menuVariantOf != null; }
}
