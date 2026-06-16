package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SeedDataValidationSmokeTest extends AcceptanceTestBase {

    @Test
    void shouldKeepSeededDevelopmentDataUsableForDemoJourney() throws Exception {
        restoreSeedDemoState();

        String ownerJwt = seedOwnerJwt();

        JsonNode restaurant = getAuthorizedJson("/api/admin/restaurant", ownerJwt);
        assertEquals("Naia Burger", restaurant.path("name").asText());
        assertEquals("naia-burger", restaurant.path("slug").asText());
        assertEquals("acct_seed_test", restaurant.path("paymentProviderAccountId").asText());

        JsonNode publicMenu = getJson("/api/public/menu/naia-burger");
        assertTrue(containsMenuItem(publicMenu, BURGER_MENU_ID.toString()));
        assertTrue(containsMenuItem(publicMenu, BACON_MENU_ID.toString()));
        assertTrue(containsMenuItem(publicMenu, FRIES_ID.toString()));
        assertTrue(containsMenuItem(publicMenu, COKE_ID.toString()));

        JsonNode createdOrder = createSeedDemoOrder();

        JsonNode order = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("en_attente_paiement", order.path("status").asText());
        assertEquals(0, new BigDecimal("25.30").compareTo(order.path("total").decimalValue()));
        assertEquals(4, order.path("items").size());
    }
}
