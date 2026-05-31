package com.qrrestaurant.order.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.composition.InMemoryMenuCompositionRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.order.domain.OrderItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class OrderPricingServiceTest {

    private InMemoryMenuItemRepository menuItemRepository;
    private InMemoryMenuCompositionRepository menuCompositionRepository;
    private InMemoryCategoryRepository categoryRepository;

    private OrderPricingService orderPricingService;

    @BeforeEach
    void setUp() {
        menuItemRepository = new InMemoryMenuItemRepository();
        menuCompositionRepository = new InMemoryMenuCompositionRepository();
        categoryRepository = new InMemoryCategoryRepository();
        orderPricingService = new OrderPricingService(menuItemRepository, menuCompositionRepository, categoryRepository);
    }

    @Test
    void shouldPriceNewOrderUsingMenuRepositoriesAndConcreteCatalogData() {
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID groupId = UUID.randomUUID();

        MenuItem menuMain = new MenuItem(UUID.randomUUID(), categoryId, "Menu burger", null,
                new BigDecimal("10.00"), null, true, UUID.randomUUID());
        MenuItem fries = new MenuItem(UUID.randomUUID(), categoryId, "Frites", null,
                new BigDecimal("3.50"), null, true, null);
        MenuItem drink = new MenuItem(UUID.randomUUID(), categoryId, "Coca", null,
                new BigDecimal("2.50"), null, true, null);
        Category category = new Category(categoryId, restaurantId, "Burgers", null, 0, true);

        menuItemRepository.save(menuMain);
        menuItemRepository.save(fries);
        menuItemRepository.save(drink);
        categoryRepository.save(category);
        menuCompositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.accompagnement, fries.getId(), new BigDecimal("0.50")));
        menuCompositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.boisson, drink.getId(), BigDecimal.ZERO));

        OrderPricingService.PricingResult result = orderPricingService.priceNewOrder(restaurantId, List.of(
                new CreateOrderUseCase.OrderItemRequest(menuMain.getId(), 2, groupId, "plat"),
                new CreateOrderUseCase.OrderItemRequest(fries.getId(), 2, groupId, "accompagnement"),
                new CreateOrderUseCase.OrderItemRequest(drink.getId(), 2, groupId, "boisson")
        ));

        List<OrderItem> pricedItems = result.items();
        assertEquals(new BigDecimal("21.00"), result.total());
        assertEquals(new BigDecimal("10.00"), pricedItems.get(0).getUnitPrice());
        assertEquals(new BigDecimal("0.50"), pricedItems.get(1).getUnitPrice());
        assertEquals(BigDecimal.ZERO, pricedItems.get(2).getUnitPrice());
    }

    @Test
    void shouldRepriceExistingOrderAndKeepTheSameOrderItems() {
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID menuGroupId = UUID.randomUUID();

        MenuItem menuMain = new MenuItem(UUID.randomUUID(), categoryId, "Menu bacon", null,
                new BigDecimal("12.00"), null, true, UUID.randomUUID());
        MenuItem nuggets = new MenuItem(UUID.randomUUID(), categoryId, "Nuggets", null,
                new BigDecimal("4.00"), null, true, null);
        MenuItem drink = new MenuItem(UUID.randomUUID(), categoryId, "Coca", null,
                new BigDecimal("2.50"), null, true, null);
        categoryRepository.save(new Category(categoryId, restaurantId, "Menus", null, 0, true));
        menuItemRepository.save(menuMain);
        menuItemRepository.save(nuggets);
        menuItemRepository.save(drink);
        menuCompositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.accompagnement, nuggets.getId(), new BigDecimal("1.50")));
        menuCompositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.boisson, drink.getId(), BigDecimal.ZERO));

        OrderItem mainItem = new OrderItem(UUID.randomUUID(), UUID.randomUUID(), menuMain.getId(),
                "Menu bacon", 1, new BigDecimal("99.99"), menuGroupId, "plat");
        OrderItem nuggetsItem = new OrderItem(UUID.randomUUID(), mainItem.getOrderId(), nuggets.getId(),
                "Nuggets", 1, new BigDecimal("99.99"), menuGroupId, "accompagnement");
        OrderItem drinkItem = new OrderItem(UUID.randomUUID(), mainItem.getOrderId(), drink.getId(),
                "Coca", 1, new BigDecimal("99.99"), menuGroupId, "boisson");

        OrderPricingService.PricingResult result =
                orderPricingService.repriceExistingOrder(restaurantId, List.of(mainItem, nuggetsItem, drinkItem));

        assertEquals(new BigDecimal("13.50"), result.total());
        assertEquals(new BigDecimal("12.00"), result.items().get(0).getUnitPrice());
        assertEquals(new BigDecimal("1.50"), result.items().get(1).getUnitPrice());
        assertEquals(BigDecimal.ZERO, result.items().get(2).getUnitPrice());
    }
}
