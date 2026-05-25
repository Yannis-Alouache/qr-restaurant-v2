package com.qrrestaurant.restaurant.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface RestaurantJpaRepository extends JpaRepository<RestaurantJpaEntity, UUID> {
    Optional<RestaurantJpaEntity> findBySlug(String slug);
    Optional<RestaurantJpaEntity> findByUserId(UUID userId);
    boolean existsBySlug(String slug);
}
