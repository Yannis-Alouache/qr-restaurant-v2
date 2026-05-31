package com.qrrestaurant.menu.infrastructure.persistence.category;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface CategoryJpaRepository extends JpaRepository<CategoryJpaEntity, UUID> {
    List<CategoryJpaEntity> findByRestaurantIdOrderByPosition(UUID restaurantId);
}
