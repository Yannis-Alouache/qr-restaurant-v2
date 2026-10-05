package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.application.SendOrderConfirmationUseCase;
import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderConfirmationEmail;
import com.qrrestaurant.order.domain.OrderConfirmationMailer;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.item.InMemoryOrderItemRepository;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.restaurant.infrastructure.persistence.table.InMemoryRestaurantTableRepository;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import com.qrrestaurant.shared.infrastructure.events.RecordingMessageChannel;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class HandleWebhookUseCaseIntegrationTest {

    private static final String CLIENT_BASE_URL = "http://localhost:4300";

    private final InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
    private final InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
    private final InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
    private final InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
    private final RecordingOrderConfirmationMailer mailer = new RecordingOrderConfirmationMailer();

    private final HandleWebhookUseCase handleWebhookUseCase = new HandleWebhookUseCase(
            orderRepository,
            new OrderEventPublisher(new SimpMessagingTemplate(new RecordingMessageChannel())),
            new SendOrderConfirmationUseCase(mailer, orderItemRepository,
                    restaurantRepository, tableRepository, CLIENT_BASE_URL));

    @Test
    void shouldMarkTheOrderAsPaidWhenCheckoutCompletes() {
        UUID restaurantId = seedRestaurant("Le Comptoir du Marché", "classique");
        Order savedOrder = seedPaidOrder(restaurantId);

        handleWebhookUseCase.handleCheckoutCompleted(
                savedOrder.getId().toString(), "pi_123456", "client@example.com");

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.nouvelle, updatedOrder.getStatus());
        assertEquals("pi_123456", updatedOrder.getPaymentTransactionId());
        assertEquals("client@example.com", updatedOrder.getCustomerEmail());
    }

    @Test
    void shouldSendTheThemeAwareReceiptWhenCheckoutCompletes() {
        UUID restaurantId = seedRestaurant("Le Comptoir du Marché", "chaud");
        Order savedOrder = seedPaidOrder(restaurantId);

        handleWebhookUseCase.handleCheckoutCompleted(
                savedOrder.getId().toString(), "pi_123456", "client@example.com");

        assertEquals(1, mailer.received.size());
        OrderConfirmationEmail email = mailer.received.get(0);
        assertEquals("client@example.com", email.to());
        assertEquals(orderReference(savedOrder.getId()), email.orderReference());
        assertEquals("Le Comptoir du Marché", email.restaurantName());
        assertEquals("chaud", email.themeId());
        assertEquals(12, email.tableNumber());
        assertEquals(new BigDecimal("18.00"), email.total());
        assertEquals(2, email.items().size());
        assertEquals("Burger Maison", email.items().get(0).name());
        assertEquals(CLIENT_BASE_URL + "/order/" + savedOrder.getId() + "/confirmation", email.trackingUrl());
        assertNotNull(email.paidAt());
    }

    @Test
    void shouldSendExactlyOneReceiptWhenCheckoutCompletionIsDeliveredTwice() {
        UUID restaurantId = seedRestaurant("Le Comptoir du Marché", "classique");
        Order savedOrder = seedPaidOrder(restaurantId);

        handleWebhookUseCase.handleCheckoutCompleted(
                savedOrder.getId().toString(), "pi_123456", "client@example.com");
        handleWebhookUseCase.handleCheckoutCompleted(
                savedOrder.getId().toString(), "pi_123456", "client@example.com");

        assertEquals(1, mailer.received.size());
    }

    @Test
    void shouldConfirmPaymentWithoutReceiptWhenNoCustomerEmailWasCollected() {
        UUID restaurantId = seedRestaurant("Le Comptoir du Marché", "classique");
        Order savedOrder = seedPaidOrder(restaurantId);

        handleWebhookUseCase.handleCheckoutCompleted(savedOrder.getId().toString(), "pi_123456", null);

        assertEquals(OrderStatus.nouvelle, orderRepository.findById(savedOrder.getId()).orElseThrow().getStatus());
        assertEquals(0, mailer.received.size());
    }

    @Test
    void shouldConfirmPaymentWithoutReceiptWhenRestaurantIsUnknown() {
        Order savedOrder = seedPaidOrder(UUID.randomUUID());

        handleWebhookUseCase.handleCheckoutCompleted(
                savedOrder.getId().toString(), "pi_123456", "client@example.com");

        assertEquals(OrderStatus.nouvelle, orderRepository.findById(savedOrder.getId()).orElseThrow().getStatus());
        assertEquals(0, mailer.received.size());
    }

    @Test
    void shouldMarkTheOrderAsFailedWhenCheckoutExpires() {
        UUID restaurantId = seedRestaurant("Le Comptoir du Marché", "classique");
        Order savedOrder = seedPaidOrder(restaurantId);

        handleWebhookUseCase.handleCheckoutExpired(savedOrder.getId().toString());

        Order updatedOrder = orderRepository.findById(savedOrder.getId()).orElseThrow();
        assertEquals(OrderStatus.paiement_echoue, updatedOrder.getStatus());
        assertNull(updatedOrder.getPaymentTransactionId());
        assertEquals(0, mailer.received.size());
    }

    private UUID seedRestaurant(String name, String themeId) {
        UUID restaurantId = UUID.randomUUID();
        restaurantRepository.save(Restaurant.from(restaurantId, UUID.randomUUID(), name,
                "slug-" + restaurantId, null, null, null, themeId, null, null));
        return restaurantId;
    }

    private Order seedPaidOrder(UUID restaurantId) {
        UUID tableId = UUID.randomUUID();
        tableRepository.save(RestaurantTable.from(tableId, restaurantId, 12));

        Order order = orderRepository.save(Order.create(restaurantId, tableId, new BigDecimal("18.00")));

        OrderItem burger = OrderItem.create(UUID.randomUUID(), "Burger Maison", 1,
                new BigDecimal("14.00"), null, null);
        burger.assignToOrder(order.getId());
        OrderItem fries = OrderItem.create(UUID.randomUUID(), "Frites maison", 2,
                new BigDecimal("2.00"), null, null);
        fries.assignToOrder(order.getId());
        orderItemRepository.saveAll(List.of(burger, fries));
        return order;
    }

    private static String orderReference(UUID orderId) {
        String hex = orderId.toString().replace("-", "").toUpperCase();
        return hex.substring(0, 4) + "-" + hex.substring(hex.length() - 2);
    }

    private static class RecordingOrderConfirmationMailer implements OrderConfirmationMailer {
        private final List<OrderConfirmationEmail> received = new ArrayList<>();

        @Override
        public void send(OrderConfirmationEmail email) {
            assertFalse(received.stream().anyMatch(e -> e.orderReference().equals(email.orderReference())),
                    "Le reçu doit partir une seule fois par commande");
            received.add(email);
        }
    }
}
