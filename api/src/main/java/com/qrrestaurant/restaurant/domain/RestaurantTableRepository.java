package com.qrrestaurant.restaurant.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RestaurantTableRepository {
    RestaurantTable save(RestaurantTable table);
    Optional<RestaurantTable> findById(UUID id);
    List<RestaurantTable> findByRestaurantIdOrderByNumber(UUID restaurantId);
}
