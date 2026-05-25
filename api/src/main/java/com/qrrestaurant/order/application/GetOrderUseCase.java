package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.order.domain.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetOrderUseCase {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public GetOrderUseCase(OrderRepository orderRepository, OrderItemRepository orderItemRepository) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
    }

    public OrderDetailResponse execute(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(OrderNotFoundException::new);
        List<OrderItem> items = orderItemRepository.findByOrderId(orderId);
        List<OrderItemResponse> itemResponses = items.stream()
                .map(i -> new OrderItemResponse(i.getId().toString(), i.getMenuItemId().toString(),
                        i.getName(), i.getQuantity(), i.getUnitPrice(),
                        i.getMenuGroupId() != null ? i.getMenuGroupId().toString() : null,
                        i.getMenuRole()))
                .toList();
        return new OrderDetailResponse(order.getId().toString(), order.getStatus().name(),
                order.getTotal(), order.getCreatedAt().toString(), itemResponses);
    }

    public record OrderDetailResponse(String id, String status, java.math.BigDecimal total,
                                       String createdAt, List<OrderItemResponse> items) {}
    public record OrderItemResponse(String id, String menuItemId, String name, int quantity,
                                     java.math.BigDecimal unitPrice, String menuGroupId, String menuRole) {}

    public static class OrderNotFoundException extends RuntimeException {
        public OrderNotFoundException() { super("Commande introuvable"); }
    }
}
