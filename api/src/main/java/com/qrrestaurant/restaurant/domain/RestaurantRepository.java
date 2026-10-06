package com.qrrestaurant.restaurant.domain;

import java.util.Optional;
import java.util.UUID;

public interface RestaurantRepository {
    Restaurant save(Restaurant restaurant);
    Optional<Restaurant> findById(UUID id);
    Optional<Restaurant> findBySlug(String slug);
    Optional<Restaurant> findByUserId(UUID userId);
    Optional<Restaurant> findByPaymentProviderAccountId(String paymentProviderAccountId);
    boolean existsBySlug(String slug);
}
