package com.qrrestaurant.menu.domain;

import java.util.Objects;
import java.util.UUID;

public class Category {

    private final UUID id;
    private final UUID restaurantId;
    private String name;
    private String imagePath;
    private Integer position;
    private boolean hasMenu;

    private Category(UUID id, UUID restaurantId, String name, String imagePath, Integer position, boolean hasMenu) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.imagePath = imagePath;
        this.position = position;
        this.hasMenu = hasMenu;
    }

    public static Category create(UUID restaurantId, String name, String imagePath, Integer position, boolean hasMenu) {
        Objects.requireNonNull(restaurantId, "restaurantId");
        Objects.requireNonNull(name, "name");
        return new Category(UUID.randomUUID(), restaurantId, name, imagePath,
                position != null ? position : 0, hasMenu);
    }

    public static Category from(UUID id, UUID restaurantId, String name, String imagePath, Integer position, boolean hasMenu) {
        return new Category(id, restaurantId, name, imagePath, position, hasMenu);
    }

    public void update(String name, String imagePath, Integer position, Boolean hasMenu) {
        if (name != null) this.name = name;
        if (imagePath != null) this.imagePath = imagePath;
        if (position != null) this.position = position;
        if (hasMenu != null) this.hasMenu = hasMenu;
    }

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public String getName() { return name; }
    public String getImagePath() { return imagePath; }
    public Integer getPosition() { return position; }
    public boolean isHasMenu() { return hasMenu; }
}
