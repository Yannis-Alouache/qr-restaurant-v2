package com.qrrestaurant.order.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.composition.InMemoryMenuCompositionRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.infrastructure.persistence.item.InMemoryOrderItemRepository;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.restaurant.infrastructure.persistence.table.InMemoryRestaurantTableRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CreateOrderUseCaseIntegrationTest {

    @Test
    void shouldCreateAnOrderWithVerifiedComboAndStandalonePrices() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID comboGroupId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();

        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                tableRepository,
                new OrderPricingService(menuItemRepository, compositionRepository, categoryRepository));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setSlug("naia-burger");
        restaurantRepository.save(restaurant);

        tableRepository.save(new RestaurantTable(tableId, restaurantId, 1));
        categoryRepository.save(new Category(categoryId, restaurantId, "Menus", null, 0, true));

        UUID burgerMenuId = UUID.randomUUID();
        UUID friesId = UUID.randomUUID();
        UUID cokeId = UUID.randomUUID();
        UUID brownieId = UUID.randomUUID();

        menuItemRepository.save(new MenuItem(burgerMenuId, categoryId, "Menu burger", null,
                new BigDecimal("10.40"), null, true, UUID.randomUUID()));
        menuItemRepository.save(new MenuItem(friesId, categoryId, "Frites", null,
                new BigDecimal("3.00"), null, true, null));
        menuItemRepository.save(new MenuItem(cokeId, categoryId, "Coca", null,
                new BigDecimal("2.50"), null, true, null));
        menuItemRepository.save(new MenuItem(brownieId, categoryId, "Brownie", null,
                new BigDecimal("4.50"), null, true, null));
        compositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.accompagnement, friesId, BigDecimal.ZERO));
        compositionRepository.save(new MenuComposition(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.boisson, cokeId, BigDecimal.ZERO));

        CreateOrderUseCase.OrderResponse response = createOrderUseCase.execute("naia-burger", tableId, List.of(
                new CreateOrderUseCase.OrderItemRequest(burgerMenuId, 2, comboGroupId, "plat"),
                new CreateOrderUseCase.OrderItemRequest(friesId, 2, comboGroupId, "accompagnement"),
                new CreateOrderUseCase.OrderItemRequest(cokeId, 2, comboGroupId, "boisson"),
                new CreateOrderUseCase.OrderItemRequest(brownieId, 1, null, null)
        ));

        Map<UUID, OrderItem> savedItemsByMenuItemId = orderItemRepository.findByOrderId(UUID.fromString(response.id())).stream()
                .collect(Collectors.toMap(OrderItem::getMenuItemId, Function.identity()));

        assertEquals(new BigDecimal("25.30"), response.total());
        assertEquals(4, savedItemsByMenuItemId.size());
        assertEquals(new BigDecimal("10.40"), savedItemsByMenuItemId.get(burgerMenuId).getUnitPrice());
        assertTrue(savedItemsByMenuItemId.get(friesId).getUnitPrice().compareTo(BigDecimal.ZERO) == 0);
        assertTrue(savedItemsByMenuItemId.get(cokeId).getUnitPrice().compareTo(BigDecimal.ZERO) == 0);
        assertEquals(new BigDecimal("4.50"), savedItemsByMenuItemId.get(brownieId).getUnitPrice());
    }

    @Test
    void shouldRejectTableWhenItDoesNotBelongToTheRestaurant() {
        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();

        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                tableRepository,
                new OrderPricingService(menuItemRepository, compositionRepository, categoryRepository));

        Restaurant restaurant = new Restaurant();
        restaurant.setId(UUID.randomUUID());
        restaurant.setSlug("naia-burger");
        restaurantRepository.save(restaurant);

        assertThrows(IllegalArgumentException.class, () -> createOrderUseCase.execute(
                "naia-burger",
                UUID.randomUUID(),
                List.of()));
    }
}
