package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.shared.infrastructure.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class HandleWebhookUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;

    public HandleWebhookUseCase(OrderRepository orderRepository, OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
    }

    public void handleCheckoutCompleted(String orderId, String paymentTransactionId) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable: " + orderId));

        order.markCheckoutCompleted(paymentTransactionId);
        orderRepository.save(order);

        eventPublisher.publishOrderUpdate(order.getRestaurantId(), order.getId(), OrderStatus.nouvelle.name());
    }

    public void handleCheckoutExpired(String orderId) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable: " + orderId));

        order.markCheckoutExpired();
        orderRepository.save(order);

        eventPublisher.publishOrderUpdate(order.getRestaurantId(), order.getId(), OrderStatus.paiement_echoue.name());
    }
}
