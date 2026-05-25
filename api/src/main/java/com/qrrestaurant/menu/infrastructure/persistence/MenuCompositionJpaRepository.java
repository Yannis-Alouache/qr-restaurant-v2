package com.qrrestaurant.menu.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface MenuCompositionJpaRepository extends JpaRepository<MenuCompositionJpaEntity, UUID> {
    List<MenuCompositionJpaEntity> findByRestaurantId(UUID restaurantId);
    List<MenuCompositionJpaEntity> findByRestaurantIdAndCompositionType(UUID restaurantId, String type);
}
