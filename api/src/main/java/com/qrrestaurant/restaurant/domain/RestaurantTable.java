package com.qrrestaurant.restaurant.domain;

import java.util.Objects;
import java.util.UUID;

public class RestaurantTable {

    private final UUID id;
    private final UUID restaurantId;
    private final Integer number;

    private RestaurantTable(UUID id, UUID restaurantId, Integer number) {
        this.id = id;
        this.restaurantId = restaurantId;
        this.number = number;
    }

    public static RestaurantTable create(UUID restaurantId, Integer number) {
        Objects.requireNonNull(restaurantId, "restaurantId");
        return new RestaurantTable(null, restaurantId, number);
    }

    public static RestaurantTable from(UUID id, UUID restaurantId, Integer number) {
        return new RestaurantTable(id, restaurantId, number);
    }

    public UUID getId() { return id; }
    public UUID getRestaurantId() { return restaurantId; }
    public Integer getNumber() { return number; }
}
