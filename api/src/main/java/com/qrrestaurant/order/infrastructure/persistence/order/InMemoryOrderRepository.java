package com.qrrestaurant.order.infrastructure.persistence.order;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryOrderRepository implements OrderRepository {
    private final Map<UUID, Order> orders = new LinkedHashMap<>();

    @Override
    public Order save(Order order) {
        UUID id = order.getId() != null ? order.getId() : UUID.randomUUID();
        Instant createdAt = order.getCreatedAt() != null ? order.getCreatedAt() : Instant.now();

        Order saved = Order.from(id, order.getRestaurantId(), order.getTableId(), order.getStatus(),
                order.getTotal(), order.getPaymentTransactionId(), createdAt);
        orders.put(id, saved);
        return copy(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return Optional.ofNullable(orders.get(id)).map(this::copy);
    }

    @Override
    public List<Order> findByRestaurantIdAndStatusIn(UUID restaurantId, List<OrderStatus> statuses) {
        return orders.values().stream()
                .filter(order -> restaurantId.equals(order.getRestaurantId()))
                .filter(order -> statuses.contains(order.getStatus()))
                .map(this::copy)
                .toList();
    }

    private Order copy(Order order) {
        return Order.from(order.getId(), order.getRestaurantId(), order.getTableId(), order.getStatus(),
                order.getTotal(), order.getPaymentTransactionId(), order.getCreatedAt());
    }
}
