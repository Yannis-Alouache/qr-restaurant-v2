package com.qrrestaurant.order.infrastructure.persistence.order;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class OrderRepositoryAdapter implements OrderRepository {

    private final OrderJpaRepository jpaRepo;

    public OrderRepositoryAdapter(OrderJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Order save(Order order) {
        OrderJpaEntity entity = toEntity(order);
        OrderJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Order> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<Order> findByRestaurantIdAndStatusIn(UUID restaurantId, List<OrderStatus> statuses) {
        List<String> statusStrings = statuses.stream().map(OrderStatus::name).toList();
        return jpaRepo.findByRestaurantIdAndStatusIn(restaurantId, statusStrings)
                .stream().map(this::toDomain).toList();
    }

    private Order toDomain(OrderJpaEntity e) {
        return new Order(e.getId(), e.getRestaurantId(), e.getTableId(),
                OrderStatus.valueOf(e.getStatus()), e.getTotal(),
                e.getPaymentTransactionId(), e.getCreatedAt());
    }

    private OrderJpaEntity toEntity(Order d) {
        OrderJpaEntity e = new OrderJpaEntity();
        e.setId(d.getId());
        e.setRestaurantId(d.getRestaurantId());
        e.setTableId(d.getTableId());
        e.setStatus(d.getStatus().name());
        e.setTotal(d.getTotal());
        e.setPaymentTransactionId(d.getPaymentTransactionId());
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
