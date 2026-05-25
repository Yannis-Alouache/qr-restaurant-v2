package com.qrrestaurant.restaurant.domain;

import java.util.List;
import java.util.UUID;

public interface RestaurantTableRepository {
    RestaurantTable save(RestaurantTable table);
    List<RestaurantTable> findByRestaurantIdOrderByNumber(UUID restaurantId);
}
