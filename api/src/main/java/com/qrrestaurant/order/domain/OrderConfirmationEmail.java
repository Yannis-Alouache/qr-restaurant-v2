package com.qrrestaurant.order.domain;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/**
 * Données du reçu client : assemblées au moment du paiement confirmé,
 * rendues en HTML « ticket » par l'infrastructure mail. Les montants sont
 * des prix unitaires : le rendu calcule les totaux de ligne.
 */
public record OrderConfirmationEmail(
        String to,
        String orderReference,
        String restaurantName,
        String themeId,
        String restaurantAddress,
        Integer tableNumber,
        Instant paidAt,
        BigDecimal total,
        List<Line> items,
        String trackingUrl) {

    public OrderConfirmationEmail {
        if (items == null) {
            items = List.of();
        }
        items = List.copyOf(items);
    }

    public record Line(String name, int quantity, BigDecimal unitPrice) {}
}
