package com.qrrestaurant.order.infrastructure.persistence;

import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class OrderItemRepositoryAdapter implements OrderItemRepository {

    private final OrderItemJpaRepository jpaRepo;

    public OrderItemRepositoryAdapter(OrderItemJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<OrderItem> saveAll(List<OrderItem> items) {
        List<OrderItemJpaEntity> entities = items.stream().map(this::toEntity).toList();
        List<OrderItemJpaEntity> saved = jpaRepo.saveAll(entities);
        return saved.stream().map(this::toDomain).toList();
    }

    @Override
    public List<OrderItem> findByOrderId(UUID orderId) {
        return jpaRepo.findByOrderId(orderId).stream().map(this::toDomain).toList();
    }

    private OrderItem toDomain(OrderItemJpaEntity e) {
        return new OrderItem(e.getId(), e.getOrderId(), e.getMenuItemId(),
                e.getName(), e.getQuantity(), e.getUnitPrice(),
                e.getMenuGroupId(), e.getMenuRole());
    }

    private OrderItemJpaEntity toEntity(OrderItem d) {
        OrderItemJpaEntity e = new OrderItemJpaEntity();
        e.setId(d.getId());
        e.setOrderId(d.getOrderId());
        e.setMenuItemId(d.getMenuItemId());
        e.setName(d.getName());
        e.setQuantity(d.getQuantity());
        e.setUnitPrice(d.getUnitPrice());
        e.setMenuGroupId(d.getMenuGroupId());
        e.setMenuRole(d.getMenuRole());
        return e;
    }
}
