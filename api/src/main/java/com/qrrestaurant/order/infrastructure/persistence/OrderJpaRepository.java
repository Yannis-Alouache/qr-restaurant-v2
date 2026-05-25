package com.qrrestaurant.order.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OrderJpaRepository extends JpaRepository<OrderJpaEntity, UUID> {
    List<OrderJpaEntity> findByRestaurantIdAndStatusIn(UUID restaurantId, List<String> statuses);
}
