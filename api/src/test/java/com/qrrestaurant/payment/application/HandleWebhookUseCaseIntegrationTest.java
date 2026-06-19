package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import com.qrrestaurant.shared.infrastructure.events.PublishedMessage;
import com.qrrestaurant.shared.infrastructure.events.RecordingMessageChannel;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HandleWebhookUseCaseIntegrationTest {

    @Test
    void shouldMarkTheOrderAsPaidWhenCheckoutCompletes() {
        UUID restaurantId = UUID.randomUUID();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        RecordingMessageChannel messageChannel = new RecordingMessageChannel();
        HandleWebhookUseCase handleWebhookUseCase = new HandleWebhookUseCase(
                orderRepository,
                new OrderEventPublisher(new SimpMessagingTemplate(messageChannel)));

        Order order = Order.create(restaurantId, UUID.randomUUID(), new BigDecimal("10.40"));
        Order savedOrder = orderRepository.save(order);

        handleWebhookUseCase.handleCheckoutCompleted(savedOrder.getId().toString(), "pi_123456");

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.nouvelle, updatedOrder.getStatus());
        assertEquals("pi_123456", updatedOrder.getPaymentTransactionId());
        assertEquals(List.of(
                new PublishedMessage("/topic/restaurants/" + restaurantId + "/orders",
                        new com.qrrestaurant.order.application.dto.OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.nouvelle.name())),
                new PublishedMessage("/topic/orders/" + savedOrder.getId(),
                        new com.qrrestaurant.order.application.dto.OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.nouvelle.name()))
        ), messageChannel.publishedMessages());
    }

    @Test
    void shouldMarkTheOrderAsFailedWhenCheckoutExpires() {
        UUID restaurantId = UUID.randomUUID();
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        RecordingMessageChannel messageChannel = new RecordingMessageChannel();
        HandleWebhookUseCase handleWebhookUseCase = new HandleWebhookUseCase(
                orderRepository,
                new OrderEventPublisher(new SimpMessagingTemplate(messageChannel)));

        Order order = Order.create(restaurantId, UUID.randomUUID(), new BigDecimal("10.40"));
        Order savedOrder = orderRepository.save(order);

        handleWebhookUseCase.handleCheckoutExpired(savedOrder.getId().toString());

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.paiement_echoue, updatedOrder.getStatus());
        assertEquals(List.of(
                new PublishedMessage("/topic/restaurants/" + restaurantId + "/orders",
                        new com.qrrestaurant.order.application.dto.OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.paiement_echoue.name())),
                new PublishedMessage("/topic/orders/" + savedOrder.getId(),
                        new com.qrrestaurant.order.application.dto.OrderStatusUpdate(savedOrder.getId().toString(), OrderStatus.paiement_echoue.name()))
        ), messageChannel.publishedMessages());
    }
}
