package com.qrrestaurant.order.infrastructure.persistence.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

interface OrderItemJpaRepository extends JpaRepository<OrderItemJpaEntity, UUID> {
    List<OrderItemJpaEntity> findByOrderId(UUID orderId);
}
