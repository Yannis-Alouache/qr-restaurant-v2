package com.qrrestaurant.menu.domain;

import java.math.BigDecimal;
import java.util.Objects;
import java.util.UUID;

public class MenuItem {

    private final UUID id;
    private final UUID categoryId;
    private String name;
    private String description;
    private BigDecimal price;
    private String imagePath;
    private boolean available;
    private UUID menuVariantOf;

    private MenuItem(UUID id, UUID categoryId, String name, String description,
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

    public static MenuItem create(UUID categoryId, String name, String description,
                                  BigDecimal price, String imagePath, UUID menuVariantOf) {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        return new MenuItem(UUID.randomUUID(), categoryId, name, normalizeDescription(description),
                price, imagePath, true, menuVariantOf);
    }

    public static MenuItem from(UUID id, UUID categoryId, String name, String description,
                                BigDecimal price, String imagePath, boolean available, UUID menuVariantOf) {
        return new MenuItem(id, categoryId, name, description, price, imagePath, available, menuVariantOf);
    }

    public void update(String name, String description, BigDecimal price, String imagePath, UUID menuVariantOf) {
        if (name != null) this.name = name;
        if (description != null) this.description = normalizeDescription(description);
        if (price != null) this.price = price;
        if (imagePath != null) this.imagePath = imagePath;
        if (menuVariantOf != null) this.menuVariantOf = menuVariantOf;
    }

    public void changeAvailability(boolean available) {
        this.available = available;
    }

    public boolean isMenuVariant() { return menuVariantOf != null; }

    private static String normalizeDescription(String description) {
        if (description == null || description.isBlank()) {
            return null;
        }
        return description;
    }

    public UUID getId() { return id; }
    public UUID getCategoryId() { return categoryId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public BigDecimal getPrice() { return price; }
    public String getImagePath() { return imagePath; }
    public boolean isAvailable() { return available; }
    public UUID getMenuVariantOf() { return menuVariantOf; }
}
