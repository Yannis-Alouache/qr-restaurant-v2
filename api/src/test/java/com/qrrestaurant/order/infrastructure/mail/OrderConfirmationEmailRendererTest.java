package com.qrrestaurant.order.infrastructure.mail;

import com.qrrestaurant.order.domain.OrderConfirmationEmail;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderConfirmationEmailRendererTest {

    private final OrderConfirmationEmailRenderer renderer = new OrderConfirmationEmailRenderer();

    @Test
    void shouldRenderTheRestaurantThemeAccent() {
        String html = renderer.render(sample("chaud"));

        assertTrue(html.contains("#DD503F"), "accent chaud attendu (monogramme)");
        assertTrue(html.contains("#C21725"), "bouton aux couleurs du thème chaud");
        assertFalse(html.contains("#008A6A"), "l'accent du thème classique ne doit pas fuiter");
    }

    @Test
    void shouldRenderEverySupportedThemePalette() {
        assertTrue(renderer.render(sample("classique")).contains("background-color:#008A6A"));
        assertTrue(renderer.render(sample("nature")).contains("background-color:#48773E"));
        assertTrue(renderer.render(sample("elegant")).contains("background-color:#643185"));
    }

    @Test
    void shouldFallBackToClassiqueThemeWhenThemeIsMissingOrUnknown() {
        String missing = renderer.render(sample(null));
        String unknown = renderer.render(sample("neon"));

        assertTrue(missing.contains("#008A6A"));
        assertTrue(unknown.contains("#008A6A"));
    }

    @Test
    void shouldFormatPricesTheFrenchWay() {
        String html = renderer.render(sample("classique"));

        assertTrue(html.contains("18,00\u00A0€"), "total au format français");
        assertTrue(html.contains("4,00\u00A0€"), "montant de ligne calculé (2 × 2,00)");
        assertTrue(html.contains("2,00\u00A0€ l'unité"), "prix unitaire affiché pour les quantités > 1");
    }

    @Test
    void shouldEscapeUserGeneratedContent() {
        OrderConfirmationEmail hostile = new OrderConfirmationEmail(
                "client@example.com", "AB12-CD", "L'<Atelier> & Fils", "classique",
                "12 rue des <Acacias>", 12, Instant.parse("2026-10-04T10:47:00Z"),
                new BigDecimal("18.00"),
                List.of(new OrderConfirmationEmail.Line("Menu « Spécial » & frites", 1, new BigDecimal("18.00"))),
                "http://localhost:4300/order/x/confirmation");

        String html = renderer.render(hostile);

        assertFalse(html.contains("L'<Atelier>"), "le nom du restaurant doit être échappé");
        assertFalse(html.contains("« Spécial » & frites"), "le nom de l'article doit être échappé");
        assertTrue(html.contains("Menu « Spécial » &amp; frites"));
        assertTrue(html.contains("12 rue des &lt;Acacias&gt;"));
    }

    @Test
    void shouldDeriveTheMonogramFromTheRestaurantName() {
        assertEquals("CM", OrderConfirmationEmailRenderer.monogram("Le Comptoir du Marché"));
        assertEquals("NB", OrderConfirmationEmailRenderer.monogram("Naia Burger"));
        assertEquals("QR", OrderConfirmationEmailRenderer.monogram("   "));
    }

    @Test
    void shouldOmitTheTableRowWhenTheOrderHasNoTable() {
        OrderConfirmationEmail noTable = new OrderConfirmationEmail(
                "client@example.com", "AB12-CD", "Naia Burger", "classique",
                null, null, Instant.parse("2026-10-04T10:47:00Z"),
                new BigDecimal("18.00"), List.of(), "http://localhost:4300/order/x/confirmation");

        String html = renderer.render(noTable);

        assertTrue(html.contains(">COMMANDE<"));
        assertFalse(html.contains(">TABLE<"));
    }

    @Test
    void shouldLeaveNoUnresolvedTemplateToken() {
        String html = renderer.render(sample("classique"));

        assertFalse(html.matches("(?s).*__[A-Z_]+__.*"),
                "un token de template n'a pas été remplacé");
    }

    private static OrderConfirmationEmail sample(String themeId) {
        return new OrderConfirmationEmail(
                "client@example.com", "A3F2-9C", "Le Comptoir du Marché", themeId,
                "12 rue des Acacias, 75011 Paris", 12, Instant.parse("2026-10-04T10:47:00Z"),
                new BigDecimal("18.00"),
                List.of(
                        new OrderConfirmationEmail.Line("Burger Maison", 1, new BigDecimal("14.00")),
                        new OrderConfirmationEmail.Line("Frites maison", 2, new BigDecimal("2.00"))),
                "http://localhost:4300/order/xyz/confirmation");
    }
}
