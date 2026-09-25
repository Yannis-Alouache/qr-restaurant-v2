package com.qrrestaurant.order.infrastructure.persistence;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Transactional
class OrderPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private OrderItemRepository orderItemRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    private UUID restaurantId;
    private UUID tableId;

    /**
     * Restaurant dédié au test (rollbacké avec la transaction) : les autres
     * classes de test HTTP commitent des commandes "nouvelle" sur le restaurant
     * seedé — compter ses commandes rendrait ce test dépendant de l'ordre
     * d'exécution des classes.
     */
    @BeforeEach
    void seedIsolatedRestaurant() {
        restaurantId = UUID.randomUUID();
        tableId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, 'not-a-login-hash')",
                userId, "it-" + userId + "@test.local");
        jdbcTemplate.update(
                "INSERT INTO restaurant (id, user_id, name, slug, theme_id) VALUES (?, ?, 'Resto IT', ?, 'classique')",
                restaurantId, userId, "it-" + restaurantId);
        jdbcTemplate.update(
                "INSERT INTO restaurant_table (id, restaurant_id, number) VALUES (?, ?, 1)",
                tableId, restaurantId);
    }

    @Test
    void shouldPersistAndReloadOrdersAndOrderItemsWithPostgres() {
        Order order = Order.from(null, restaurantId, tableId, OrderStatus.nouvelle,
                new BigDecimal("14.90"), null, null);
        Order savedOrder = orderRepository.save(order);

        OrderItem brownie = OrderItem.create(BROWNIE_ID, "Brownie maison", 2, new BigDecimal("4.50"), null, null);
        brownie.assignToOrder(savedOrder.getId());

        OrderItem coke = OrderItem.create(COKE_ID, "Coca-Cola", 1, new BigDecimal("2.50"), null, null);
        coke.assignToOrder(savedOrder.getId());

        orderItemRepository.saveAll(List.of(brownie, coke));

        Order reloadedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        List<OrderItem> reloadedItems = orderItemRepository.findByOrderId(savedOrder.getId());
        List<Order> activeOrders = orderRepository.findByRestaurantIdAndStatusIn(restaurantId, List.of(OrderStatus.nouvelle));

        assertEquals(OrderStatus.nouvelle, reloadedOrder.getStatus());
        assertEquals(new BigDecimal("14.90"), reloadedOrder.getTotal());
        assertEquals(2, reloadedItems.size());
        assertEquals(1, activeOrders.size());
        assertEquals(savedOrder.getId(), activeOrders.get(0).getId());
    }
}
