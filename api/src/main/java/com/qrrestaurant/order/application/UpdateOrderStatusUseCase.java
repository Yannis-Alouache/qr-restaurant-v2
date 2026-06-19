package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateOrderStatusUseCase {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderEventPublisher eventPublisher;

    public UpdateOrderStatusUseCase(OrderRepository orderRepository,
                                     RestaurantRepository restaurantRepository,
                                     OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.eventPublisher = eventPublisher;
    }

    public void execute(UUID userId, UUID orderId, String targetStatus) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);

        if (!order.getRestaurantId().equals(restaurant.getId())) {
            throw new OrderNotFoundException();
        }

        OrderStatus status = OrderStatus.valueOf(targetStatus);
        order.advanceForAdministration(status);
        orderRepository.save(order);

        eventPublisher.publishOrderUpdate(restaurant.getId(), orderId, targetStatus);
    }

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant trouvé"); }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException() { super("Commande introuvable"); }
    }
}
