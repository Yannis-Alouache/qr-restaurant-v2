package com.qrrestaurant.order.domain;

import java.util.List;
import java.util.UUID;

public interface OrderItemRepository {
    List<OrderItem> saveAll(List<OrderItem> items);
    List<OrderItem> findByOrderId(UUID orderId);
}
