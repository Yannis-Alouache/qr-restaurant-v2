package com.qrrestaurant.payment.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.composition.InMemoryMenuCompositionRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.order.application.CreateOrderUseCase;
import com.qrrestaurant.order.application.OrderPricingService;
import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.item.InMemoryOrderItemRepository;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.payment.infrastructure.gateway.DeterministicPaymentGateway;
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

class CreateCheckoutSessionUseCaseIntegrationTest {

    @Test
    void shouldRepriceTheOrderBeforeCreatingStripeCheckout() {
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

        OrderPricingService orderPricingService = new OrderPricingService(
                menuItemRepository, compositionRepository, categoryRepository);
        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                orderRepository, orderItemRepository, restaurantRepository, tableRepository, orderPricingService);
        CreateCheckoutSessionUseCase createCheckoutSessionUseCase = new CreateCheckoutSessionUseCase(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                orderPricingService,
                new DeterministicPaymentGateway(),
                "http://localhost:4300");

        restaurantRepository.save(newRestaurant(restaurantId, "naia-burger", "acct_seed_test"));
        tableRepository.save(com.qrrestaurant.restaurant.domain.RestaurantTable.from(tableId, restaurantId, 1));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Menus", null, 0, true));

        UUID baconMenuId = UUID.randomUUID();
        UUID nuggetsId = UUID.randomUUID();
        UUID cokeId = UUID.randomUUID();
        menuItemRepository.save(MenuItem.from(baconMenuId, categoryId, "Menu bacon", null,
                new BigDecimal("12.00"), null, true, UUID.randomUUID()));
        menuItemRepository.save(MenuItem.from(nuggetsId, categoryId, "Nuggets", null,
                new BigDecimal("4.00"), null, true, null));
        menuItemRepository.save(MenuItem.from(cokeId, categoryId, "Coca", null,
                new BigDecimal("2.50"), null, true, null));
        compositionRepository.save(MenuComposition.from(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.accompagnement, nuggetsId, new BigDecimal("1.50")));
        compositionRepository.save(MenuComposition.from(UUID.randomUUID(), restaurantId,
                MenuComposition.CompositionType.boisson, cokeId, BigDecimal.ZERO));

        CreateOrderUseCase.OrderResponse orderResponse = createOrderUseCase.execute("naia-burger", tableId, List.of(
                new CreateOrderUseCase.OrderItemRequest(baconMenuId, 1, comboGroupId, "plat"),
                new CreateOrderUseCase.OrderItemRequest(nuggetsId, 1, comboGroupId, "accompagnement"),
                new CreateOrderUseCase.OrderItemRequest(cokeId, 1, comboGroupId, "boisson")
        ));

        UUID orderId = UUID.fromString(orderResponse.id());
        List<OrderItem> tamperedItems = orderItemRepository.findByOrderId(orderId);
        tamperedItems.forEach(item -> item.reprice(new BigDecimal("99.99")));
        orderItemRepository.saveAll(tamperedItems);

        Order tamperedOrder = orderRepository.findById(orderId).orElseThrow();
        tamperedOrder.setTotal(new BigDecimal("299.97"));
        orderRepository.save(tamperedOrder);

        CreateCheckoutSessionUseCase.CheckoutSessionResponse response = createCheckoutSessionUseCase.execute(orderId);

        Order repricedOrder = orderRepository.findById(orderId).orElseThrow();
        Map<UUID, OrderItem> repricedItemsByMenuItemId = orderItemRepository.findByOrderId(orderId).stream()
                .collect(Collectors.toMap(OrderItem::getMenuItemId, Function.identity()));

        assertEquals("https://checkout.test/session/%s?amount=13.50&account=acct_seed_test".formatted(orderId), response.checkoutUrl());
        assertEquals(new BigDecimal("13.50"), repricedOrder.getTotal());
        assertEquals(new BigDecimal("12.00"), repricedItemsByMenuItemId.get(baconMenuId).getUnitPrice());
        assertEquals(new BigDecimal("1.50"), repricedItemsByMenuItemId.get(nuggetsId).getUnitPrice());
        assertTrue(repricedItemsByMenuItemId.get(cokeId).getUnitPrice().compareTo(BigDecimal.ZERO) == 0);
    }

    @Test
    void shouldReopenPaymentLifecycleWhenRetryingCheckoutAfterFailure() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();

        OrderPricingService orderPricingService = new OrderPricingService(
                menuItemRepository, compositionRepository, categoryRepository);
        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                orderRepository, orderItemRepository, restaurantRepository, tableRepository, orderPricingService);
        CreateCheckoutSessionUseCase createCheckoutSessionUseCase = new CreateCheckoutSessionUseCase(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                orderPricingService,
                new DeterministicPaymentGateway(),
                "http://localhost:4300");

        restaurantRepository.save(newRestaurant(restaurantId, "naia-burger", "acct_seed_test"));
        tableRepository.save(com.qrrestaurant.restaurant.domain.RestaurantTable.from(tableId, restaurantId, 1));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));

        UUID burgerId = UUID.randomUUID();
        menuItemRepository.save(MenuItem.from(burgerId, categoryId, "Burger", null,
                new BigDecimal("12.00"), null, true, null));

        CreateOrderUseCase.OrderResponse orderResponse = createOrderUseCase.execute("naia-burger", tableId, List.of(
                new CreateOrderUseCase.OrderItemRequest(burgerId, 1, null, null)
        ));

        UUID orderId = UUID.fromString(orderResponse.id());
        Order failedOrder = orderRepository.findById(orderId).orElseThrow();
        failedOrder.markCheckoutExpired();
        orderRepository.save(failedOrder);

        CreateCheckoutSessionUseCase.CheckoutSessionResponse response = createCheckoutSessionUseCase.execute(orderId);

        Order reopenedOrder = orderRepository.findById(orderId).orElseThrow();
        assertEquals(OrderStatus.en_attente_paiement, reopenedOrder.getStatus());
        assertEquals("https://checkout.test/session/%s?amount=12.00&account=acct_seed_test".formatted(orderId), response.checkoutUrl());
    }

    @Test
    void shouldRejectCheckoutWhenRestaurantPaymentAccountIsMissing() {
        UUID restaurantId = UUID.randomUUID();
        UUID tableId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
        InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();

        OrderPricingService orderPricingService = new OrderPricingService(
                menuItemRepository, compositionRepository, categoryRepository);
        CreateOrderUseCase createOrderUseCase = new CreateOrderUseCase(
                orderRepository, orderItemRepository, restaurantRepository, tableRepository, orderPricingService);
        CreateCheckoutSessionUseCase createCheckoutSessionUseCase = new CreateCheckoutSessionUseCase(
                orderRepository,
                orderItemRepository,
                restaurantRepository,
                orderPricingService,
                new DeterministicPaymentGateway(),
                "http://localhost:4300");

        restaurantRepository.save(newRestaurant(restaurantId, "naia-burger", null));
        tableRepository.save(com.qrrestaurant.restaurant.domain.RestaurantTable.from(tableId, restaurantId, 1));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));

        UUID burgerId = UUID.randomUUID();
        menuItemRepository.save(MenuItem.from(burgerId, categoryId, "Burger", null,
                new BigDecimal("12.00"), null, true, null));

        CreateOrderUseCase.OrderResponse orderResponse = createOrderUseCase.execute("naia-burger", tableId, List.of(
                new CreateOrderUseCase.OrderItemRequest(burgerId, 1, null, null)
        ));

        assertThrows(com.qrrestaurant.restaurant.domain.Restaurant.PaymentNotConfiguredException.class,
                () -> createCheckoutSessionUseCase.execute(UUID.fromString(orderResponse.id())));
    }

    private com.qrrestaurant.restaurant.domain.Restaurant newRestaurant(UUID restaurantId, String slug, String accountId) {
        return com.qrrestaurant.restaurant.domain.Restaurant.from(restaurantId, null, null, slug, null, null,
                "classique", accountId, null);
    }
}
