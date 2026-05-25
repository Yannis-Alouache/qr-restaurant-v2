package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetRestaurantOrdersUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;

    public GetRestaurantOrdersUseCase(OrderRepository orderRepository,
                                      OrderItemRepository orderItemRepository,
                                      RestaurantRepository restaurantRepository,
                                      RestaurantTableRepository tableRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.tableRepository = tableRepository;
    }

    public List<OrderView> getActiveOrders(UUID userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);

        List<Order> orders = orderRepository.findByRestaurantIdAndStatusIn(
                restaurant.getId(),
                List.of(OrderStatus.nouvelle, OrderStatus.en_preparation, OrderStatus.prete));
        Map<UUID, Integer> tableNumbers = tableRepository.findByRestaurantIdOrderByNumber(restaurant.getId()).stream()
                .collect(Collectors.toMap(RestaurantTable::getId, RestaurantTable::getNumber, (first, second) -> first));

        return orders.stream().map(order -> {
            List<OrderItem> items = orderItemRepository.findByOrderId(order.getId());
            List<OrderItemView> itemViews = items.stream()
                    .map(i -> new OrderItemView(i.getId().toString(), i.getMenuItemId().toString(),
                            i.getName(), i.getQuantity(), i.getUnitPrice(),
                            i.getMenuGroupId() != null ? i.getMenuGroupId().toString() : null,
                            i.getMenuRole()))
                    .toList();
            return new OrderView(order.getId().toString(), getTableNumber(order, tableNumbers),
                    order.getStatus().name(), order.getTotal(),
                    order.getCreatedAt().toString(), itemViews);
        }).toList();
    }

    private int getTableNumber(Order order, Map<UUID, Integer> tableNumbers) {
        Integer tableNumber = tableNumbers.get(order.getTableId());
        if (tableNumber == null) {
            throw new IllegalStateException("Table introuvable pour la commande " + order.getId());
        }
        return tableNumber;
    }

    public record OrderView(String id, int tableNumber, String status, java.math.BigDecimal total,
                             String createdAt, List<OrderItemView> items) {}
    public record OrderItemView(String id, String menuItemId, String name, int quantity,
                                 java.math.BigDecimal unitPrice, String menuGroupId, String menuRole) {}

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant trouvé"); }
    }
}
