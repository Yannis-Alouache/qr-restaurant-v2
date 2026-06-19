package com.qrrestaurant.order.infrastructure.persistence.item;

import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryOrderItemRepository implements OrderItemRepository {
    private final Map<UUID, OrderItem> itemsById = new LinkedHashMap<>();
    private final Map<UUID, List<UUID>> itemIdsByOrderId = new LinkedHashMap<>();

    @Override
    public List<OrderItem> saveAll(List<OrderItem> items) {
        List<OrderItem> saved = new ArrayList<>();

        for (OrderItem item : items) {
            UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
            OrderItem stored = OrderItem.from(id, item.getOrderId(), item.getMenuItemId(), item.getName(),
                    item.getQuantity(), item.getUnitPrice(), item.getMenuGroupId(), item.getMenuRole());

            itemsById.put(id, stored);
            itemIdsByOrderId.computeIfAbsent(stored.getOrderId(), ignored -> new ArrayList<>());
            if (!itemIdsByOrderId.get(stored.getOrderId()).contains(id)) {
                itemIdsByOrderId.get(stored.getOrderId()).add(id);
            }

            saved.add(copy(stored));
        }

        return saved;
    }

    @Override
    public List<OrderItem> findByOrderId(UUID orderId) {
        return itemIdsByOrderId.getOrDefault(orderId, List.of()).stream()
                .map(itemsById::get)
                .map(this::copy)
                .toList();
    }

    private OrderItem copy(OrderItem item) {
        return OrderItem.from(item.getId(), item.getOrderId(), item.getMenuItemId(), item.getName(),
                item.getQuantity(), item.getUnitPrice(), item.getMenuGroupId(), item.getMenuRole());
    }
}
