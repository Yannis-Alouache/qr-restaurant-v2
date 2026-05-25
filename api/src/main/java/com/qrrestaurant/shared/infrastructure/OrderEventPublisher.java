package com.qrrestaurant.shared.infrastructure;

import com.qrrestaurant.order.application.OrderStatusUpdate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class OrderEventPublisher {

    private final SimpMessagingTemplate messagingTemplate;

    public OrderEventPublisher(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishOrderUpdate(UUID restaurantId, UUID orderId, String status) {
        OrderStatusUpdate update = new OrderStatusUpdate(orderId.toString(), status);

        messagingTemplate.convertAndSend(
                "/topic/restaurants/" + restaurantId + "/orders", update);

        messagingTemplate.convertAndSend(
                "/topic/orders/" + orderId, update);
    }
}
