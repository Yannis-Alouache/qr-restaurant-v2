package com.qrrestaurant.payment.application;

import com.qrrestaurant.order.application.dto.OrderStatusUpdate;
import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.payment.domain.PaymentGateway;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.shared.infrastructure.events.OrderEventPublisher;
import com.qrrestaurant.shared.infrastructure.events.PublishedMessage;
import com.qrrestaurant.shared.infrastructure.events.RecordingMessageChannel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RefundOrderUseCaseTest {

    private InMemoryOrderRepository orderRepository;
    private InMemoryRestaurantRepository restaurantRepository;
    private RecordingPaymentGateway paymentGateway;
    private RecordingMessageChannel messageChannel;
    private RefundOrderUseCase useCase;

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID ownerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        orderRepository = new InMemoryOrderRepository();
        restaurantRepository = new InMemoryRestaurantRepository();
        paymentGateway = new RecordingPaymentGateway();
        messageChannel = new RecordingMessageChannel();
        useCase = new RefundOrderUseCase(
                orderRepository,
                restaurantRepository,
                paymentGateway,
                new OrderEventPublisher(new SimpMessagingTemplate(messageChannel)));

        restaurantRepository.save(Restaurant.from(
                restaurantId, ownerId, "Naia Burger", "naia-burger", null, null, "classique", "acct_seed_test", null));
    }

    @Test
    void shouldRefundAPaidOrderAndPublishTheTransition() {
        Order order = orderRepository.save(Order.from(null, restaurantId, UUID.randomUUID(),
                OrderStatus.nouvelle, new BigDecimal("14.90"), "pi_123", null));

        useCase.execute(ownerId, order.getId());

        Order refunded = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.rembourse, refunded.getStatus());
        assertEquals("pi_123", paymentGateway.refundedPaymentIntentId);
        assertEquals("rembourse", lastPublishedStatus());
    }

    @Test
    void shouldRefundAnAlreadyServedOrderForCommercialGoodwill() {
        Order order = orderRepository.save(Order.from(null, restaurantId, UUID.randomUUID(),
                OrderStatus.servie, new BigDecimal("9.80"), "pi_456", null));

        useCase.execute(ownerId, order.getId());

        assertEquals(OrderStatus.rembourse, orderRepository.findById(order.getId()).orElseThrow().getStatus());
    }

    @Test
    void shouldRejectARefundOnAnOrderOfAnotherRestaurant() {
        UUID otherRestaurantId = UUID.randomUUID();
        Order foreignOrder = orderRepository.save(Order.from(null, otherRestaurantId, UUID.randomUUID(),
                OrderStatus.nouvelle, new BigDecimal("14.90"), "pi_789", null));

        assertThrows(RefundOrderUseCase.OrderNotFoundException.class, () -> useCase.execute(ownerId, foreignOrder.getId()));
        assertEquals(0, paymentGateway.calls);
    }

    @Test
    void shouldRejectARefundOnAnUnpaidOrder() {
        Order unpaid = orderRepository.save(Order.from(null, restaurantId, UUID.randomUUID(),
                OrderStatus.en_attente_paiement, new BigDecimal("14.90"), null, null));

        assertThrows(Order.RefundUnavailableException.class, () -> useCase.execute(ownerId, unpaid.getId()));
        assertEquals(0, paymentGateway.calls);
    }

    private String lastPublishedStatus() {
        List<PublishedMessage> published = messageChannel.publishedMessages();
        if (published.isEmpty()) {
            return null;
        }
        OrderStatusUpdate update = (OrderStatusUpdate) published.getLast().payload();
        return update.status();
    }

    private static final class RecordingPaymentGateway implements PaymentGateway {

        private int calls;
        private String refundedPaymentIntentId;

        @Override
        public String createCheckoutSession(String orderId, BigDecimal amount, String description,
                                             String destinationAccountId, String successUrl, String cancelUrl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void refundPayment(String paymentIntentId) {
            this.calls++;
            this.refundedPaymentIntentId = paymentIntentId;
        }
    }
}
