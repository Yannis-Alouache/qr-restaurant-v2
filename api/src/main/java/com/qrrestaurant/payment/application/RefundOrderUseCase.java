package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Rembourse une commande payée, à la demande du restaurateur : vérifie le
 * ownership, appelle Stripe (remboursement intégral + reversement du transfert
 * Connect), marque la commande remboursée et diffuse la transition aux clients
 * qui suivent la commande.
 */
@Service
@Transactional
public class RefundOrderUseCase {

    private final OrderRepository orderRepository;
    private final RestaurantRepository restaurantRepository;
    private final PaymentGateway paymentGateway;
    private final OrderEventPublisher eventPublisher;

    public RefundOrderUseCase(OrderRepository orderRepository,
                              RestaurantRepository restaurantRepository,
                              PaymentGateway paymentGateway,
                              OrderEventPublisher eventPublisher) {
        this.orderRepository = orderRepository;
        this.restaurantRepository = restaurantRepository;
        this.paymentGateway = paymentGateway;
        this.eventPublisher = eventPublisher;
    }

    public void execute(UUID userId, UUID orderId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        if (!order.getRestaurantId().equals(restaurant.getId())) {
            throw new OrderNotFoundException();
        }

        order.assertRefundable();

        paymentGateway.refundPayment(order.getPaymentTransactionId());
        order.markRefunded();
        orderRepository.save(order);

        eventPublisher.publishOrderUpdate(order.getRestaurantId(), order.getId(), order.getStatus().name());
    }

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant pour ce compte"); }
    }

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException() { super("Commande introuvable"); }
    }
}
