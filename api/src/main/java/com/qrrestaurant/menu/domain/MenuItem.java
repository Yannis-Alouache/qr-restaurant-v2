package com.qrrestaurant.menu.domain;

import java.math.BigDecimal;
import java.time.Instant;
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
    private Instant deletedAt;

    private MenuItem(UUID id, UUID categoryId, String name, String description,
                     BigDecimal price, String imagePath, boolean available, UUID menuVariantOf,
                     Instant deletedAt) {
        this.id = id;
        this.categoryId = categoryId;
        this.name = name;
        this.description = description;
        this.price = price;
        this.imagePath = imagePath;
        this.available = available;
        this.menuVariantOf = menuVariantOf;
        this.deletedAt = deletedAt;
    }

    public static MenuItem create(UUID categoryId, String name, String description,
                                  BigDecimal price, String imagePath, UUID menuVariantOf) {
        Objects.requireNonNull(categoryId, "categoryId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(price, "price");
        return new MenuItem(null, categoryId, name, normalizeDescription(description),
                price, imagePath, true, menuVariantOf, null);
    }

    public static MenuItem from(UUID id, UUID categoryId, String name, String description,
                                BigDecimal price, String imagePath, boolean available, UUID menuVariantOf) {
        return from(id, categoryId, name, description, price, imagePath, available, menuVariantOf, null);
    }

    public static MenuItem from(UUID id, UUID categoryId, String name, String description,
                                BigDecimal price, String imagePath, boolean available, UUID menuVariantOf,
                                Instant deletedAt) {
        return new MenuItem(id, categoryId, name, description, price, imagePath, available,
                menuVariantOf, deletedAt);
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

    /**
     * Suppression logique : la ligne reste en base pour l'historique des commandes
     * (order_item référence menu_item en ON DELETE RESTRICT) mais disparaît de
     * tous les chemins de lecture.
     */
    public void delete() {
        if (deletedAt == null) {
            this.deletedAt = Instant.now();
        }
    }

    public boolean isDeleted() { return deletedAt != null; }
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
    public Instant getDeletedAt() { return deletedAt; }
}
