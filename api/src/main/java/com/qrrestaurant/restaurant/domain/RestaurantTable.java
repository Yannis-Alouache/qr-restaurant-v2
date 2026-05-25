package com.qrrestaurant.restaurant.domain;

import java.util.UUID;

public class RestaurantTable {

    private UUID id;
    private UUID restaurantId;
    private Integer number;

    public RestaurantTable() {}

    public RestaurantTable(UUID id, UUID restaurantId, Integer number) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.number = number;
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getRestaurantId() { return restaurantId; }
    public void setRestaurantId(UUID restaurantId) { this.restaurantId = restaurantId; }

    public Integer getNumber() { return number; }
    public void setNumber(Integer number) { this.number = number; }
}
