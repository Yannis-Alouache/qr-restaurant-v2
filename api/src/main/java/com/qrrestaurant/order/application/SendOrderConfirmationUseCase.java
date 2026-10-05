package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderConfirmationEmail;
import com.qrrestaurant.order.domain.OrderConfirmationMailer;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Assemble et déclenche l'envoi du reçu client après un paiement confirmé.
 * Totalement best effort : le client a payé et voit sa commande à l'écran,
 * aucun échec ici (restaurant introuvable, panne SMTP, bug de rendu) ne doit
 * remonter au webhook qui a déjà confirmé le paiement.
 */
@Service
public class SendOrderConfirmationUseCase {

    private static final Logger log = LoggerFactory.getLogger(SendOrderConfirmationUseCase.class);

    private final OrderConfirmationMailer mailer;
    private final OrderItemRepository orderItemRepository;
    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final String clientBaseUrl;

    public SendOrderConfirmationUseCase(OrderConfirmationMailer mailer,
                                        OrderItemRepository orderItemRepository,
                                        RestaurantRepository restaurantRepository,
                                        RestaurantTableRepository tableRepository,
                                        @Value("${app.client-base-url}") String clientBaseUrl) {
        this.mailer = mailer;
        this.orderItemRepository = orderItemRepository;
        this.restaurantRepository = restaurantRepository;
        this.tableRepository = tableRepository;
        this.clientBaseUrl = clientBaseUrl;
    }

    public void sendFor(Order paidOrder) {
        try {
            if (paidOrder.getCustomerEmail() == null || paidOrder.getCustomerEmail().isBlank()) {
                return;
            }
            Restaurant restaurant = restaurantRepository.findById(paidOrder.getRestaurantId()).orElse(null);
            if (restaurant == null) {
                log.warn("Reçu de commande {} ignoré : restaurant {} introuvable",
                        paidOrder.getId(), paidOrder.getRestaurantId());
                return;
            }
            mailer.send(buildEmail(paidOrder, restaurant));
        } catch (RuntimeException e) {
            log.error("Envoi du reçu de commande {} impossible : {}", paidOrder.getId(), e.getMessage());
        }
    }

    private OrderConfirmationEmail buildEmail(Order order, Restaurant restaurant) {
        Integer tableNumber = tableRepository.findById(order.getTableId())
                .map(RestaurantTable::getNumber)
                .orElse(null);
        List<OrderConfirmationEmail.Line> lines = orderItemRepository.findByOrderId(order.getId()).stream()
                .map(SendOrderConfirmationUseCase::toLine)
                .toList();
        return new OrderConfirmationEmail(
                order.getCustomerEmail(),
                orderReference(order.getId()),
                restaurant.getName(),
                restaurant.getThemeId(),
                restaurant.getAddress(),
                tableNumber,
                Instant.now(),
                order.getTotal(),
                lines,
                trackingUrl(order.getId()));
    }

    private static OrderConfirmationEmail.Line toLine(OrderItem item) {
        return new OrderConfirmationEmail.Line(item.getName(), item.getQuantity(), item.getUnitPrice());
    }

    /**
     * Référence courte et stable pour que le client puisse la citer au
     * comptoir : l'UUID complet est illisible sur un reçu.
     */
    private static String orderReference(UUID orderId) {
        String hex = orderId.toString().replace("-", "").toUpperCase();
        return hex.substring(0, 4) + "-" + hex.substring(hex.length() - 2);
    }

    private String trackingUrl(UUID orderId) {
        String base = clientBaseUrl.endsWith("/") ? clientBaseUrl.substring(0, clientBaseUrl.length() - 1) : clientBaseUrl;
        return base + "/order/" + orderId + "/confirmation";
    }
}
