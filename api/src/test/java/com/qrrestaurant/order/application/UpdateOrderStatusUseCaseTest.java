package com.qrrestaurant.order.application;
import com.qrrestaurant.order.application.dto.OrderStatusUpdate;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import com.qrrestaurant.shared.infrastructure.events.PublishedMessage;
import com.qrrestaurant.shared.infrastructure.events.RecordingMessageChannel;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateOrderStatusUseCaseTest {

    @Test
    void shouldUpdateOrderStatusAndPublishObservableUpdates() {
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        RecordingMessageChannel messageChannel = new RecordingMessageChannel();
        UpdateOrderStatusUseCase useCase = new UpdateOrderStatusUseCase(
                orderRepository,
                restaurantRepository,
                new OrderEventPublisher(new SimpMessagingTemplate(messageChannel)));

        Restaurant restaurant = Restaurant.from(restaurantId, userId, null, null, null, null, "classique", null, null);
        restaurantRepository.save(restaurant);

        Order order = Order.from(null, restaurantId, UUID.randomUUID(), OrderStatus.nouvelle,
                new BigDecimal("14.90"), null, null);
        Order savedOrder = orderRepository.save(order);

        useCase.execute(userId, savedOrder.getId(), OrderStatus.en_preparation.name());
        useCase.execute(userId, savedOrder.getId(), OrderStatus.prete.name());

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();

        assertEquals(OrderStatus.prete, updatedOrder.getStatus());
        assertEquals(List.of(
                new PublishedMessage("/topic/restaurants/" + restaurantId + "/orders",
                        new OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.en_preparation.name())),
                new PublishedMessage("/topic/orders/" + savedOrder.getId(),
                        new OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.en_preparation.name())),
                new PublishedMessage("/topic/restaurants/" + restaurantId + "/orders",
                        new OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.prete.name())),
                new PublishedMessage("/topic/orders/" + savedOrder.getId(),
                        new OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.prete.name()))
        ), messageChannel.publishedMessages());
    }

    @Test
    void shouldRejectStatusUpdateWhenOrderBelongsToAnotherRestaurant() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        UpdateOrderStatusUseCase useCase = new UpdateOrderStatusUseCase(
                orderRepository,
                restaurantRepository,
                new OrderEventPublisher(new SimpMessagingTemplate(new RecordingMessageChannel())));

        Restaurant restaurant = Restaurant.from(UUID.randomUUID(), UUID.randomUUID(), null, null, null, null, "classique", null, null);
        restaurantRepository.save(restaurant);

        Order order = Order.create(UUID.randomUUID(), UUID.randomUUID(), new BigDecimal("14.90"));
        Order savedOrder = orderRepository.save(order);

        assertThrows(UpdateOrderStatusUseCase.OrderNotFoundException.class,
                () -> useCase.execute(restaurant.getUserId(), savedOrder.getId(), OrderStatus.nouvelle.name()));
    }

    @Test
    void shouldRejectStatusUpdateWhenPaymentIsNotConfirmed() {
        UUID restaurantId = UUID.randomUUID();
        UUID userId = UUID.randomUUID();

        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        UpdateOrderStatusUseCase useCase = new UpdateOrderStatusUseCase(
                orderRepository,
                restaurantRepository,
                new OrderEventPublisher(new SimpMessagingTemplate(new RecordingMessageChannel())));

        Restaurant restaurant = Restaurant.from(restaurantId, userId, null, null, null, null, "classique", null, null);
        restaurantRepository.save(restaurant);

        Order order = Order.create(restaurantId, UUID.randomUUID(), new BigDecimal("14.90"));
        Order savedOrder = orderRepository.save(order);

        assertThrows(Order.UnpaidOrderStatusUpdateException.class,
                () -> useCase.execute(userId, savedOrder.getId(), OrderStatus.nouvelle.name()));
        assertEquals(OrderStatus.en_attente_paiement,
                orderRepository.findById(savedOrder.getId()).orElseThrow().getStatus());
    }
}
