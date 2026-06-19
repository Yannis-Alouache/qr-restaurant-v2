package com.qrrestaurant.order.infrastructure.persistence;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class OrderPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;

    @Test
    void shouldPersistAndReloadOrdersAndOrderItemsWithPostgres() {
        Order order = new Order();
        order.setRestaurantId(RESTAURANT_ID);
        order.setTableId(TABLE_1_ID);
        order.setStatus(OrderStatus.nouvelle);
        order.setTotal(new BigDecimal("14.90"));
        Order savedOrder = orderRepository.save(order);

        OrderItem brownie = OrderItem.create(BROWNIE_ID, "Brownie maison", 2, new BigDecimal("4.50"), null, null);
        brownie.assignToOrder(savedOrder.getId());

        OrderItem coke = OrderItem.create(COKE_ID, "Coca-Cola", 1, new BigDecimal("2.50"), null, null);
        coke.assignToOrder(savedOrder.getId());

        orderItemRepository.saveAll(List.of(brownie, coke));

        Order reloadedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        List<OrderItem> reloadedItems = orderItemRepository.findByOrderId(savedOrder.getId());
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusIn(RESTAURANT_ID, List.of(OrderStatus.nouvelle));

        assertEquals(OrderStatus.nouvelle, reloadedOrder.getStatus());
        assertEquals(new BigDecimal("14.90"), reloadedOrder.getTotal());
        assertEquals(2, reloadedItems.size());
        assertEquals(1, activeOrders.size());
        assertEquals(savedOrder.getId(), activeOrders.get(0).getId());
    }
}
