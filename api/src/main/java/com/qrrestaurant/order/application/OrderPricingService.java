package com.qrrestaurant.order.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuCompositionRepository;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;
import com.qrrestaurant.order.domain.OrderPricingPolicy;
import com.qrrestaurant.order.domain.OrderItem;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class OrderPricingService {

    private final MenuItemRepository menuItemRepository;
    private final MenuCompositionRepository menuCompositionRepository;
    private final CategoryRepository categoryRepository;
    private final OrderPricingPolicy orderPricingPolicy;

    @Autowired
    public OrderPricingService(MenuItemRepository menuItemRepository,
                               MenuCompositionRepository menuCompositionRepository,
                               CategoryRepository categoryRepository) {
        this(menuItemRepository, menuCompositionRepository, categoryRepository, new OrderPricingPolicy());
    }

    OrderPricingService(MenuItemRepository menuItemRepository,
                        MenuCompositionRepository menuCompositionRepository,
                        CategoryRepository categoryRepository,
                        OrderPricingPolicy orderPricingPolicy) {
        this.menuItemRepository = menuItemRepository;
        this.menuCompositionRepository = menuCompositionRepository;
        this.categoryRepository = categoryRepository;
        this.orderPricingPolicy = orderPricingPolicy;
    }

    public PricingResult priceNewOrder(UUID restaurantId, List<CreateOrderUseCase.OrderItemRequest> requestedItems) {
        OrderPricingPolicy.OrderMenuCatalog catalog = loadCatalog(requestedItems.stream()
                .map(CreateOrderUseCase.OrderItemRequest::menuItemId)
                .toList(), restaurantId);

        OrderPricingPolicy.PricingResult pricingResult = orderPricingPolicy.priceNewOrder(
                restaurantId,
                requestedItems.stream()
                        .map(item -> new OrderPricingPolicy.RequestItem(
                                item.menuItemId(), item.quantity(), item.menuGroupId(), item.menuRole()))
                        .toList(),
                catalog);

        return new PricingResult(pricingResult.items(), pricingResult.total());
    }

    public PricingResult repriceExistingOrder(UUID restaurantId, List<OrderItem> existingItems) {
        OrderPricingPolicy.OrderMenuCatalog catalog = loadCatalog(existingItems.stream()
                .map(OrderItem::getMenuItemId)
                .toList(), restaurantId);

        OrderPricingPolicy.PricingResult pricingResult =
                orderPricingPolicy.repriceExistingOrder(restaurantId, existingItems, catalog);
        return new PricingResult(pricingResult.items(), pricingResult.total());
    }

    private OrderPricingPolicy.OrderMenuCatalog loadCatalog(List<UUID> menuItemIds, UUID restaurantId) {
        Map<UUID, MenuItem> menuItemsById = menuItemRepository.findAllById(menuItemIds.stream().distinct().toList()).stream()
                .collect(Collectors.toMap(MenuItem::getId, menuItem -> menuItem));

        Map<UUID, Category> categoriesById = new HashMap<>();
        for (MenuItem item : menuItemsById.values()) {
            categoriesById.computeIfAbsent(item.getCategoryId(), categoryId -> categoryRepository.findById(categoryId)
                    .orElse(null));
        }
        return new OrderPricingPolicy.OrderMenuCatalog(
                menuItemsById,
                categoriesById,
                menuCompositionRepository.findByRestaurantId(restaurantId));
    }

    public record PricingResult(List<OrderItem> items, BigDecimal total) {
    }
}
