package com.qrrestaurant.menu.domain;

import java.util.UUID;

public class Category {

    private UUID id;
    private UUID restaurantId;
    private String name;
    private String imagePath;
    private Integer position = 0;
    private boolean hasMenu = false;

    public Category() {}

    public Category(UUID id, UUID restaurantId, String name, String imagePath, Integer position, boolean hasMenu) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.name = name;
        this.imagePath = imagePath;
        this.position = position;
        this.hasMenu = hasMenu;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getImagePath() { return imagePath; }
    public void setImagePath(String imagePath) { this.imagePath = imagePath; }

    public Integer getPosition() { return position; }
    public void setPosition(Integer position) { this.position = position; }

    public boolean isHasMenu() { return hasMenu; }
    public void setHasMenu(boolean hasMenu) { this.hasMenu = hasMenu; }
}
