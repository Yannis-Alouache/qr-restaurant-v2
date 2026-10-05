package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.application.SendOrderConfirmationUseCase;
import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class HandleWebhookUseCase {

    private final OrderRepository orderRepository;
    private final OrderEventPublisher eventPublisher;
    private final SendOrderConfirmationUseCase sendOrderConfirmation;

    public HandleWebhookUseCase(OrderRepository orderRepository,
                                OrderEventPublisher eventPublisher,
                                SendOrderConfirmationUseCase sendOrderConfirmation) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.sendOrderConfirmation = sendOrderConfirmation;
    }

    public void handleCheckoutCompleted(String orderId, String paymentTransactionId, String customerEmail) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable: " + orderId));

        // Livraison en double : rien à sauver ni diffuser, on acquitte simplement.
        if (order.markCheckoutCompleted(paymentTransactionId, customerEmail)) {
            orderRepository.save(order);
            eventPublisher.publishOrderUpdate(order.getRestaurantId(), order.getId(), OrderStatus.nouvelle.name());
            // Best effort : le client a payé et suit sa commande à l'écran, un
            // échec du reçu ne doit pas invalider la confirmation de paiement.
            sendOrderConfirmation.sendFor(order);
        }
    }

    public void handleCheckoutExpired(String orderId) {
        Order order = orderRepository.findById(UUID.fromString(orderId))
                .orElseThrow(() -> new IllegalArgumentException("Commande introuvable: " + orderId));

        if (order.markCheckoutExpired()) {
            orderRepository.save(order);
            eventPublisher.publishOrderUpdate(order.getRestaurantId(), order.getId(), OrderStatus.paiement_echoue.name());
        }
    }
}
