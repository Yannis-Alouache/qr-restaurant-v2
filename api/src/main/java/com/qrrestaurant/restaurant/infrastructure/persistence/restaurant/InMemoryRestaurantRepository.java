package com.qrrestaurant.restaurant.infrastructure.persistence.restaurant;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryRestaurantRepository implements RestaurantRepository {
    private final Map<UUID, Restaurant> restaurants = new LinkedHashMap<>();

    @Override
    public Restaurant save(Restaurant restaurant) {
        UUID id = restaurant.getId() != null ? restaurant.getId() : UUID.randomUUID();
        LocalDateTime createdAt = restaurant.getCreatedAt() != null ? restaurant.getCreatedAt() : LocalDateTime.now();

        Restaurant saved = Restaurant.from(id, restaurant.getUserId(), restaurant.getName(), restaurant.getSlug(),
                restaurant.getAddress(), restaurant.getLogoPath(), restaurant.getThemeId(),
                restaurant.getPaymentProviderAccountId(), createdAt);
        restaurants.put(id, saved);
        return copy(saved);
    }

    @Override
    public Optional<Restaurant> findById(UUID id) {
        return Optional.ofNullable(restaurants.get(id)).map(this::copy);
    }

    @Override
    public Optional<Restaurant> findBySlug(String slug) {
        return restaurants.values().stream()
                .filter(restaurant -> slug.equals(restaurant.getSlug()))
                .findFirst()
                .map(this::copy);
    }

    @Override
    public Optional<Restaurant> findByUserId(UUID userId) {
        return restaurants.values().stream()
                .filter(restaurant -> userId.equals(restaurant.getUserId()))
                .findFirst()
                .map(this::copy);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return restaurants.values().stream().anyMatch(restaurant -> slug.equals(restaurant.getSlug()));
    }

    private Restaurant copy(Restaurant restaurant) {
        return Restaurant.from(restaurant.getId(), restaurant.getUserId(), restaurant.getName(), restaurant.getSlug(),
                restaurant.getAddress(), restaurant.getLogoPath(), restaurant.getThemeId(),
                restaurant.getPaymentProviderAccountId(), restaurant.getCreatedAt());
    }
}
