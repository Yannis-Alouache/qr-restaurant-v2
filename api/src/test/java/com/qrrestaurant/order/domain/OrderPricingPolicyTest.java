package com.qrrestaurant.order.domain;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.order.domain.OrderPricingPolicy.OrderMenuCatalog;
import com.qrrestaurant.order.domain.OrderPricingPolicy.RequestItem;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OrderPricingPolicyTest {

    private final OrderPricingPolicy pricingPolicy = new OrderPricingPolicy();
    private final UUID restaurantId = UUID.randomUUID();
    private final UUID otherRestaurantId = UUID.randomUUID();
    private final UUID categoryId = UUID.randomUUID();
    private final UUID comboGroupId = UUID.randomUUID();

    @Test
    void shouldPriceStandaloneItemsWithTheirMenuPrice() {
        MenuItem brownie = menuItem("Brownie", new BigDecimal("4.50"), false, true);
        OrderMenuCatalog catalog = catalog(List.of(brownie), List.of(category(restaurantId)), List.of());

        OrderPricingPolicy.PricingResult result = pricingPolicy.priceNewOrder(restaurantId, List.of(
                new RequestItem(brownie.getId(), 2, null, null)
        ), catalog);

        assertEquals(new BigDecimal("9.00"), result.total());
        assertEquals(new BigDecimal("4.50"), result.items().getFirst().getUnitPrice());
    }

    @Test
    void shouldUseSupplementPricesForComboSideAndDrinkItems() {
        MenuItem burgerMenu = menuItem("Menu burger", new BigDecimal("10.00"), true, true);
        MenuItem fries = menuItem("Frites", new BigDecimal("3.50"), false, true);
        MenuItem drink = menuItem("Coca", new BigDecimal("2.50"), false, true);

        OrderMenuCatalog catalog = catalog(
                List.of(burgerMenu, fries, drink),
                List.of(category(restaurantId)),
                List.of(
                        composition(MenuComposition.CompositionType.accompagnement, fries.getId(), new BigDecimal("0.50")),
                        composition(MenuComposition.CompositionType.boisson, drink.getId(), BigDecimal.ZERO)
                ));

        OrderPricingPolicy.PricingResult result = pricingPolicy.priceNewOrder(restaurantId, List.of(
                new RequestItem(burgerMenu.getId(), 2, comboGroupId, "plat"),
                new RequestItem(fries.getId(), 2, comboGroupId, "accompagnement"),
                new RequestItem(drink.getId(), 2, comboGroupId, "boisson")
        ), catalog);

        assertEquals(new BigDecimal("21.00"), result.total());
        assertEquals(new BigDecimal("10.00"), result.items().get(0).getUnitPrice());
        assertEquals(new BigDecimal("0.50"), result.items().get(1).getUnitPrice());
        assertEquals(BigDecimal.ZERO, result.items().get(2).getUnitPrice());
    }

    @Test
    void shouldRejectItemsBelongingToAnotherRestaurant() {
        MenuItem brownie = menuItem("Brownie", new BigDecimal("4.50"), false, true);
        OrderMenuCatalog catalog = catalog(List.of(brownie), List.of(category(otherRestaurantId)), List.of());

        assertThrows(OrderPricingPolicy.ItemRestaurantMismatchException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(brownie.getId(), 1, null, null)),
                catalog));
    }

    @Test
    void shouldRejectUnavailableItems() {
        MenuItem brownie = menuItem("Brownie", new BigDecimal("4.50"), false, false);
        OrderMenuCatalog catalog = catalog(List.of(brownie), List.of(category(restaurantId)), List.of());

        assertThrows(OrderPricingPolicy.ItemUnavailableException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(brownie.getId(), 1, null, null)),
                catalog));
    }

    @Test
    void shouldRejectMissingMenuItems() {
        OrderMenuCatalog catalog = new OrderMenuCatalog(Map.of(), Map.of(), List.of());

        assertThrows(OrderPricingPolicy.MenuItemsNotFoundException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(UUID.randomUUID(), 1, null, null)),
                catalog));
    }

    @Test
    void shouldRejectOrdersWithoutItems() {
        OrderMenuCatalog catalog = new OrderMenuCatalog(Map.of(), Map.of(), List.of());

        assertThrows(OrderPricingPolicy.OrderItemsNotFoundException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(),
                catalog));
    }

    @Test
    void shouldRejectZeroQuantity() {
        MenuItem brownie = menuItem("Brownie", new BigDecimal("4.50"), false, true);
        OrderMenuCatalog catalog = catalog(List.of(brownie), List.of(category(restaurantId)), List.of());

        assertThrows(OrderPricingPolicy.InvalidQuantityException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(brownie.getId(), 0, null, null)),
                catalog));
    }

    @Test
    void shouldRejectStandaloneItemsThatProvideAMenuRole() {
        MenuItem brownie = menuItem("Brownie", new BigDecimal("4.50"), false, true);
        OrderMenuCatalog catalog = catalog(List.of(brownie), List.of(category(restaurantId)), List.of());

        assertThrows(OrderPricingPolicy.InvalidOrderItemException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(brownie.getId(), 1, null, "dessert")),
                catalog));
    }

    @Test
    void shouldRejectStandaloneMenuVariants() {
        MenuItem burgerMenu = menuItem("Menu burger", new BigDecimal("10.00"), true, true);
        OrderMenuCatalog catalog = catalog(List.of(burgerMenu), List.of(category(restaurantId)), List.of());

        assertThrows(OrderPricingPolicy.InvalidOrderItemException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(burgerMenu.getId(), 1, null, null)),
                catalog));
    }

    @Test
    void shouldRejectComboItemsWhenRoleIsBlank() {
        MenuItem burgerMenu = menuItem("Menu burger", new BigDecimal("10.00"), true, true);
        OrderMenuCatalog catalog = catalog(List.of(burgerMenu), List.of(category(restaurantId)), List.of());

        assertThrows(OrderPricingPolicy.InvalidOrderItemException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(new RequestItem(burgerMenu.getId(), 1, comboGroupId, "  ")),
                catalog));
    }

    @Test
    void shouldRejectComboGroupsWithoutAllThreeRoles() {
        MenuItem burgerMenu = menuItem("Menu burger", new BigDecimal("10.00"), true, true);
        MenuItem fries = menuItem("Frites", new BigDecimal("3.50"), false, true);
        OrderMenuCatalog catalog = catalog(
                List.of(burgerMenu, fries),
                List.of(category(restaurantId)),
                List.of(composition(MenuComposition.CompositionType.accompagnement, fries.getId(), BigDecimal.ZERO)));

        assertThrows(OrderPricingPolicy.InvalidOrderItemException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(
                        new RequestItem(burgerMenu.getId(), 1, comboGroupId, "plat"),
                        new RequestItem(fries.getId(), 1, comboGroupId, "accompagnement")
                ),
                catalog));
    }

    @Test
    void shouldRejectComboGroupsWhenQuantitiesDoNotMatch() {
        MenuItem burgerMenu = menuItem("Menu burger", new BigDecimal("10.00"), true, true);
        MenuItem fries = menuItem("Frites", new BigDecimal("3.50"), false, true);
        MenuItem drink = menuItem("Coca", new BigDecimal("2.50"), false, true);
        OrderMenuCatalog catalog = catalog(
                List.of(burgerMenu, fries, drink),
                List.of(category(restaurantId)),
                List.of(
                        composition(MenuComposition.CompositionType.accompagnement, fries.getId(), BigDecimal.ZERO),
                        composition(MenuComposition.CompositionType.boisson, drink.getId(), BigDecimal.ZERO)
                ));

        assertThrows(OrderPricingPolicy.InvalidQuantityException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(
                        new RequestItem(burgerMenu.getId(), 1, comboGroupId, "plat"),
                        new RequestItem(fries.getId(), 2, comboGroupId, "accompagnement"),
                        new RequestItem(drink.getId(), 1, comboGroupId, "boisson")
                ),
                catalog));
    }

    @Test
    void shouldRejectComboMainThatIsNotAMenuVariant() {
        MenuItem burger = menuItem("Burger", new BigDecimal("10.00"), false, true);
        MenuItem fries = menuItem("Frites", new BigDecimal("3.50"), false, true);
        MenuItem drink = menuItem("Coca", new BigDecimal("2.50"), false, true);
        OrderMenuCatalog catalog = catalog(
                List.of(burger, fries, drink),
                List.of(category(restaurantId)),
                List.of(
                        composition(MenuComposition.CompositionType.accompagnement, fries.getId(), BigDecimal.ZERO),
                        composition(MenuComposition.CompositionType.boisson, drink.getId(), BigDecimal.ZERO)
                ));

        assertThrows(OrderPricingPolicy.InvalidOrderItemException.class, () -> pricingPolicy.priceNewOrder(
                restaurantId,
                List.of(
                        new RequestItem(burger.getId(), 1, comboGroupId, "plat"),
                        new RequestItem(fries.getId(), 1, comboGroupId, "accompagnement"),
                        new RequestItem(drink.getId(), 1, comboGroupId, "boisson")
                ),
                catalog));
    }

    private OrderMenuCatalog catalog(List<MenuItem> menuItems, List<Category> categories, List<MenuComposition> compositions) {
        return new OrderMenuCatalog(
                menuItems.stream().collect(java.util.stream.Collectors.toMap(MenuItem::getId, item -> item)),
                categories.stream().collect(java.util.stream.Collectors.toMap(Category::getId, category -> category)),
                compositions
        );
    }

    private MenuItem menuItem(String name, BigDecimal price, boolean menuVariant, boolean available) {
        return MenuItem.from(UUID.randomUUID(), categoryId, name, null, price, null, available,
                menuVariant ? UUID.randomUUID() : null);
    }

    private Category category(UUID ownerRestaurantId) {
        return new Category(categoryId, ownerRestaurantId, "Burgers", null, 0, true);
    }

    private MenuComposition composition(MenuComposition.CompositionType type, UUID menuItemId, BigDecimal supplementPrice) {
        return MenuComposition.from(UUID.randomUUID(), restaurantId, type, menuItemId, supplementPrice);
    }
}
