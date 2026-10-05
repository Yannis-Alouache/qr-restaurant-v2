package com.qrrestaurant.order.application;

import com.qrrestaurant.order.domain.Order;
import com.qrrestaurant.order.domain.OrderItem;
import com.qrrestaurant.order.domain.OrderStatus;
import com.qrrestaurant.order.infrastructure.persistence.item.InMemoryOrderItemRepository;
import com.qrrestaurant.order.infrastructure.persistence.order.InMemoryOrderRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.restaurant.infrastructure.persistence.table.InMemoryRestaurantTableRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetRestaurantOrdersUseCaseTest {

    private final UUID restaurantId = UUID.randomUUID();
    private final UUID userId = UUID.randomUUID();
    private final UUID tableId = UUID.randomUUID();

    private final InMemoryOrderRepository orderRepository = new InMemoryOrderRepository();
    private final InMemoryOrderItemRepository orderItemRepository = new InMemoryOrderItemRepository();
    private final InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
    private final InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();

    private final GetRestaurantOrdersUseCase useCase = new GetRestaurantOrdersUseCase(
            orderRepository, orderItemRepository, restaurantRepository, tableRepository);

    @Test
    void shouldReturnTheWholeHistoryOfPaidOrdersIncludingServedAndRefunded() {
        seedRestaurant();

        Order nouvelle = saveOrder(OrderStatus.nouvelle);
        Order prete = saveOrder(OrderStatus.prete);
        Order servie = saveOrder(OrderStatus.servie);
        Order rembourse = saveOrder(OrderStatus.rembourse);

        List<GetRestaurantOrdersUseCase.OrderView> orders = useCase.getOrders(userId);

        assertEquals(
                List.of(nouvelle.getId(), prete.getId(), servie.getId(), rembourse.getId())
                        .stream().map(UUID::toString).toList(),
                orders.stream().map(GetRestaurantOrdersUseCase.OrderView::id).toList());
        assertEquals(List.of("nouvelle", "prete", "servie", "rembourse"),
                orders.stream().map(GetRestaurantOrdersUseCase.OrderView::status).toList());
    }

    @Test
    void shouldExcludeCartsWhosePaymentIsNotConfirmed() {
        seedRestaurant();
        saveOrder(OrderStatus.en_attente_paiement);
        saveOrder(OrderStatus.paiement_echoue);

        assertEquals(List.of(), useCase.getOrders(userId));
    }

    @Test
    void shouldMapTableNumberAndItemsIntoTheView() {
        seedRestaurant();
        Order servie = saveOrder(OrderStatus.servie);
        orderItemRepository.saveAll(List.of(OrderItem.from(
                UUID.randomUUID(), servie.getId(), UUID.randomUUID(), "Menu Burger classique",
                2, new BigDecimal("12.00"), null, "plat")));

        GetRestaurantOrdersUseCase.OrderView view = useCase.getOrders(userId).getFirst();

        assertEquals(3, view.tableNumber());
        assertEquals(0, view.total().compareTo(new BigDecimal("24.00")));
        assertEquals(1, view.items().size());
        assertEquals("Menu Burger classique", view.items().getFirst().name());
        assertEquals(2, view.items().getFirst().quantity());
        assertEquals("plat", view.items().getFirst().menuRole());
    }

    @Test
    void shouldRejectAUserWithoutRestaurant() {
        assertThrows(GetRestaurantOrdersUseCase.NoRestaurantException.class,
                () -> useCase.getOrders(userId));
    }

    private void seedRestaurant() {
        restaurantRepository.save(Restaurant.from(
                restaurantId, userId, "Naia Burger", "naia-burger", null,
                null, null, "classique", null, null));
        tableRepository.save(RestaurantTable.from(tableId, restaurantId, 3));
    }

    private Order saveOrder(OrderStatus status) {
        return orderRepository.save(Order.from(null, restaurantId, tableId, status,
                new BigDecimal("24.00"), "pi_" + status.name(), null, Instant.now()));
    }
}
