package com.qrrestaurant.restaurant.infrastructure.persistence.table;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface RestaurantTableJpaRepository extends JpaRepository<RestaurantTableJpaEntity, UUID> {
    List<RestaurantTableJpaEntity> findByRestaurantIdOrderByNumber(UUID restaurantId);
}
