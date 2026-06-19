package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class FreshOnboardingSmokeTest extends AcceptanceTestBase {

    @Test
    void shouldSupportOwnerToCustomerCriticalJourneyWithCurrentContracts() throws Exception {
        Cookie ownerToken = signUpAndLogin("phase8-smoke");
        RestaurantSetup setup = onboardRestaurantWithMenu(ownerToken, "Smoke Bistro");

        JsonNode publicMenu = getJson("/api/public/menu/" + setup.slug);
        assertEquals(setup.slug, publicMenu.path("restaurant").path("slug").asText());
        assertTrue(containsMenuItem(publicMenu, setup.baseItemId));
        assertTrue(containsMenuItem(publicMenu, setup.variantId));
        assertTrue(containsMenuItem(publicMenu, setup.sideItemId));
        assertTrue(containsMenuItem(publicMenu, setup.drinkItemId));
        assertTrue(containsComposition(publicMenu, setup.sideItemId));
        assertTrue(containsComposition(publicMenu, setup.drinkItemId));

        UUID comboGroupId = UUID.randomUUID();
        JsonNode createdOrder = postJson("/api/public/orders", """
                {
                  "slug": "%s",
                  "tableId": "%s",
                  "items": [
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "plat"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "accompagnement"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "boisson"
                    }
                  ]
                }
                """.formatted(setup.slug, setup.tableId, setup.variantId, comboGroupId,
                setup.sideItemId, comboGroupId, setup.drinkItemId, comboGroupId),
                status().isCreated());

        JsonNode order = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("en_attente_paiement", order.path("status").asText());
        assertEquals(0, new BigDecimal("17.90").compareTo(order.path("total").decimalValue()));
        assertEquals(3, order.path("items").size());
    }

    @Test
    void shouldLetANewlyOnboardedOwnerConfigurePaymentsAndReachPublicCheckout() throws Exception {
        Cookie ownerToken = signUpAndLogin("phase8-payment");
        RestaurantSetup setup = onboardRestaurantWithMenu(ownerToken, "Payment Ready Bistro");

        JsonNode restaurantBeforeConfig = getAuthorizedJson("/api/admin/restaurant", ownerToken);
        assertTrue(restaurantBeforeConfig.path("paymentProviderAccountId").isNull());

        JsonNode restaurantAfterConfig = putAuthorizedJson("/api/admin/restaurant", ownerToken, """
                {
                  "paymentProviderAccountId": "acct_onboarded_smoke"
                }
                """);
        assertEquals("acct_onboarded_smoke", restaurantAfterConfig.path("paymentProviderAccountId").asText());

        UUID comboGroupId = UUID.randomUUID();
        JsonNode createdOrder = postJson("/api/public/orders", """
                {
                  "slug": "%s",
                  "tableId": "%s",
                  "items": [
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "plat"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "accompagnement"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "boisson"
                    }
                  ]
                }
                """.formatted(setup.slug, setup.tableId, setup.variantId, comboGroupId,
                setup.sideItemId, comboGroupId, setup.drinkItemId, comboGroupId),
                status().isCreated());

        JsonNode checkout = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());

        assertEquals(
                "https://checkout.test/session/%s?amount=17.90&account=acct_onboarded_smoke"
                        .formatted(createdOrder.path("id").asText()),
                checkout.path("checkoutUrl").asText());
    }

    @Test
    void shouldLetANewlyOnboardedOwnerReachServedStatusAfterConfiguringPayments() throws Exception {
        Cookie ownerToken = signUpAndLogin("phase8-served");
        RestaurantSetup setup = onboardRestaurantWithMenu(ownerToken, "Served Journey Bistro");

        putAuthorizedJson("/api/admin/restaurant", ownerToken, """
                {
                  "paymentProviderAccountId": "acct_onboarded_served"
                }
                """);

        UUID comboGroupId = UUID.randomUUID();
        JsonNode createdOrder = postJson("/api/public/orders", """
                {
                  "slug": "%s",
                  "tableId": "%s",
                  "items": [
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "plat"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "accompagnement"
                    },
                    {
                      "menuItemId": "%s",
                      "quantity": 1,
                      "menuGroupId": "%s",
                      "menuRole": "boisson"
                    }
                  ]
                }
                """.formatted(setup.slug, setup.tableId, setup.variantId, comboGroupId,
                setup.sideItemId, comboGroupId, setup.drinkItemId, comboGroupId),
                status().isCreated());

        JsonNode checkout = postJson("/api/public/payments/checkout", """
                {
                  "orderId": "%s"
                }
                """.formatted(createdOrder.path("id").asText()), status().isOk());
        assertEquals(
                "https://checkout.test/session/%s?amount=17.90&account=acct_onboarded_served"
                        .formatted(createdOrder.path("id").asText()),
                checkout.path("checkoutUrl").asText());

        postStripeWebhook(checkoutCompletedPayload(createdOrder.path("id").asText(), "pi_onboarded_served"));

        JsonNode adminOrders = getAuthorizedJson("/api/admin/orders", ownerToken);
        JsonNode paidOrder = findOrder(adminOrders, createdOrder.path("id").asText());
        assertTrue(paidOrder != null);
        assertEquals("nouvelle", paidOrder.path("status").asText());

        String orderPath = "/api/admin/orders/" + createdOrder.path("id").asText() + "/status";
        patchAuthorizedStatus(orderPath, ownerToken, "en_preparation");
        patchAuthorizedStatus(orderPath, ownerToken, "prete");
        patchAuthorizedStatus(orderPath, ownerToken, "servie");

        JsonNode servedOrder = getJson("/api/public/orders/" + createdOrder.path("id").asText());
        assertEquals("servie", servedOrder.path("status").asText());
        assertTrue(!containsOrder(getAuthorizedJson("/api/admin/orders", ownerToken), createdOrder.path("id").asText()));
    }

    // ── Setup helpers ─────────────────────────────────────────────────

    private record RestaurantSetup(
            String slug, String tableId,
            String baseItemId, String variantId,
            String sideItemId, String drinkItemId
    ) {}

    private Cookie signUpAndLogin(String prefix) throws Exception {
        String email = prefix + "-" + UUID.randomUUID() + "@test.com";
        String password = "Secret123!";

        postJson("/api/auth/signup", """
                {
                  "email": "%s",
                  "password": "%s"
                }
                """.formatted(email, password), status().isCreated());
        return loginJwtCookie(email, password);
    }

    private RestaurantSetup onboardRestaurantWithMenu(Cookie ownerToken, String restaurantName) throws Exception {
        JsonNode onboarding = postAuthorizedJson("/api/admin/restaurants", ownerToken, """
                {
                  "name": "%s",
                  "tableCount": 3,
                  "themeId": "chaud"
                }
                """.formatted(restaurantName));
        String slug = onboarding.path("slug").asText();
        String tableId = onboarding.path("tables").get(0).path("id").asText();

        String categoryId = postAuthorizedJson("/api/admin/categories", ownerToken, """
                {
                  "name": "Burgers signature",
                  "hasMenu": true
                }
                """).path("id").asText();

        String baseItemId = postAuthorizedJson("/api/admin/menu-items", ownerToken, """
                {
                  "categoryId": "%s",
                  "name": "Burger fumé",
                  "description": "Steak fumé, cheddar, pickles",
                  "price": 13.90
                }
                """.formatted(categoryId)).path("id").asText();

        String variantId = postAuthorizedJson("/api/admin/menu-items", ownerToken, """
                {
                  "categoryId": "%s",
                  "name": "Menu Burger fumé",
                  "price": 17.90,
                  "menuVariantOf": "%s"
                }
                """.formatted(categoryId, baseItemId)).path("id").asText();

        String sideItemId = postAuthorizedJson("/api/admin/menu-items", ownerToken, """
                {
                  "categoryId": "%s",
                  "name": "Potatoes maison",
                  "price": 4.50
                }
                """.formatted(categoryId)).path("id").asText();

        String drinkItemId = postAuthorizedJson("/api/admin/menu-items", ownerToken, """
                {
                  "categoryId": "%s",
                  "name": "Thé glacé",
                  "price": 3.20
                }
                """.formatted(categoryId)).path("id").asText();

        postAuthorizedJson("/api/admin/compositions", ownerToken, """
                {
                  "compositionType": "accompagnement",
                  "menuItemId": "%s",
                  "supplementPrice": 0
                }
                """.formatted(sideItemId));
        postAuthorizedJson("/api/admin/compositions", ownerToken, """
                {
                  "compositionType": "boisson",
                  "menuItemId": "%s",
                  "supplementPrice": 0
                }
                """.formatted(drinkItemId));

        return new RestaurantSetup(slug, tableId, baseItemId, variantId, sideItemId, drinkItemId);
    }
}
