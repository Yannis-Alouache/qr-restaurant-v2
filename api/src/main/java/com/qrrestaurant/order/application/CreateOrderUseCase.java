package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final OrderPricingService orderPricingService;

    public CreateOrderUseCase(OrderRepository orderRepository,
                               OrderItemRepository orderItemRepository,
                               RestaurantRepository restaurantRepository,
                               RestaurantTableRepository tableRepository,
                               OrderPricingService orderPricingService) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.tableRepository = tableRepository;
        this.orderPricingService = orderPricingService;
    }

    public OrderResponse execute(String slug, UUID tableId, List<OrderItemRequest> items) {
        Restaurant restaurant = restaurantRepository.findBySlug(slug)
                .orElseThrow(RestaurantNotFoundException::new);

        validateTableBelongsToRestaurant(tableId, restaurant.getId());

        OrderPricingService.PricingResult pricing = orderPricingService.priceNewOrder(restaurant.getId(), items);

        Order order = Order.create(restaurant.getId(), tableId, pricing.total());
        Order savedOrder = orderRepository.save(order);

        for (OrderItem item : pricing.items()) {
            item.assignToOrder(savedOrder.getId());
        }
        orderItemRepository.saveAll(pricing.items());

        return new OrderResponse(savedOrder.getId().toString(), savedOrder.getStatus().name(),
                savedOrder.getTotal(), savedOrder.getCreatedAt().toString());
    }

    private void validateTableBelongsToRestaurant(UUID tableId, UUID restaurantId) {
        tableRepository.findByRestaurantIdOrderByNumber(restaurantId).stream()
                .filter(t -> t.getId().equals(tableId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Table invalide pour ce restaurant"));
    }

    public record OrderItemRequest(UUID menuItemId, int quantity, UUID menuGroupId, String menuRole) {}
    public record OrderResponse(String id, String status, BigDecimal total, String createdAt) {}

    public static class RestaurantNotFoundException extends RuntimeException {
        public RestaurantNotFoundException() { super("Restaurant introuvable"); }
    }
}
