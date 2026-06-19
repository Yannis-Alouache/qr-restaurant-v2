package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderPricingPolicy;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import com.qrrestaurant.order.application.OrderPricingService;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.dao.DataAccessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class CreateCheckoutSessionUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final OrderPricingService orderPricingService;
    private final PaymentGateway paymentGateway;
    private final String clientBaseUrl;

    public CreateCheckoutSessionUseCase(OrderRepository orderRepository,
                                         OrderItemRepository orderItemRepository,
                                         RestaurantRepository restaurantRepository,
                                         OrderPricingService orderPricingService,
                                         PaymentGateway paymentGateway,
                                         @Value("${app.client-base-url}") String clientBaseUrl) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.orderPricingService = orderPricingService;
        this.paymentGateway = paymentGateway;
        this.clientBaseUrl = clientBaseUrl;
    }

    public CheckoutSessionResponse execute(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        order.assertCanCreateCheckoutSession();

        Restaurant restaurant = restaurantRepository.findById(order.getRestaurantId())
                .orElseThrow(InvalidOrderException::new);
        restaurant.assertCanAcceptOnlinePayments();

        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        if (items.isEmpty()) {
            throw new OrderPricingPolicy.OrderItemsNotFoundException();
        }

        OrderPricingService.PricingResult repriced = orderPricingService.repriceExistingOrder(restaurant.getId(), items);

        try {
            orderItemRepository.saveAll(repriced.items());
            if (repriced.total().compareTo(order.getTotal()) != 0) {
                order.updateTotal(repriced.total());
                orderRepository.save(order);
            }
        } catch (DataAccessException ex) {
            throw new PriceUpdateException(ex);
        }

        String successUrl = clientBaseUrl + "/order/" + orderId + "/confirmation";
        String cancelUrl = clientBaseUrl + "/order/" + orderId + "/cancelled";

        String checkoutUrl = paymentGateway.createCheckoutSession(
                orderId.toString(), repriced.total(),
                "Commande #" + orderId.toString().substring(0, 8),
                restaurant.getPaymentProviderAccountId(),
                successUrl, cancelUrl);

        order.markCheckoutSessionCreated();
        orderRepository.save(order);

        return new CheckoutSessionResponse(checkoutUrl);
    }

    public record CheckoutSessionResponse(String checkoutUrl) {}

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException() { super("Commande introuvable"); }
    }

    public static class InvalidOrderException extends RuntimeException {
        public InvalidOrderException() { super("Commande invalide"); }
    }

    public static class PriceUpdateException extends RuntimeException {
        public PriceUpdateException(Throwable cause) {
            super("Erreur lors de la mise à jour du total", cause);
        }
    }
}
