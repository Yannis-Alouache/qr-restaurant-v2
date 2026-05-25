package com.qrrestaurant.order.domain;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

public class OrderPricingPolicy {

    public PricingResult priceNewOrder(UUID restaurantId, List<RequestItem> requestedItems, OrderMenuCatalog catalog) {
        List<PricingInput> inputs = requestedItems.stream()
                .map(item -> new PricingInput(item.menuItemId(), item.quantity(), item.menuGroupId(), item.menuRole()))
                .toList();

        CalculatedPricing calculated = calculate(restaurantId, inputs, catalog);
        List<OrderItem> pricedItems = calculated.items().stream()
                .map(item -> new OrderItem(
                        null,
                        null,
                        item.menuItem().getId(),
                        item.menuItem().getName(),
                        item.input().quantity(),
                        item.unitPrice(),
                        item.input().menuGroupId(),
                        item.input().menuRole()))
                .toList();

        return new PricingResult(pricedItems, calculated.total());
    }

    public PricingResult repriceExistingOrder(UUID restaurantId, List<OrderItem> existingItems, OrderMenuCatalog catalog) {
        List<PricingInput> inputs = existingItems.stream()
                .map(item -> new PricingInput(item.getMenuItemId(), item.getQuantity(), item.getMenuGroupId(), item.getMenuRole()))
                .toList();

        CalculatedPricing calculated = calculate(restaurantId, inputs, catalog);
        List<OrderItem> repricedItems = new ArrayList<>();

        for (int index = 0; index < existingItems.size(); index++) {
            OrderItem original = existingItems.get(index);
            CalculatedItem repriced = calculated.items().get(index);
            original.setUnitPrice(repriced.unitPrice());
            repricedItems.add(original);
        }

        return new PricingResult(repricedItems, calculated.total());
    }

    private CalculatedPricing calculate(UUID restaurantId, List<PricingInput> items, OrderMenuCatalog catalog) {
        if (items.isEmpty()) {
            throw new OrderItemsNotFoundException();
        }

        Map<MenuComposition.CompositionType, Map<UUID, MenuComposition>> compositionsByType =
                indexCompositions(catalog.compositions());

        List<CalculatedItem> calculatedItems = new ArrayList<>();
        for (PricingInput item : items) {
            MenuItem menuItem = catalog.menuItemsById().get(item.menuItemId());
            if (menuItem == null) {
                throw new MenuItemsNotFoundException();
            }

            Category category = catalog.categoriesById().get(menuItem.getCategoryId());
            validateRestaurantOwnership(restaurantId, category);
            validateAvailability(menuItem);

            BigDecimal unitPrice = resolveUnitPrice(item, menuItem, compositionsByType);
            calculatedItems.add(new CalculatedItem(item, menuItem, unitPrice));
        }

        validateComboGroups(calculatedItems);

        BigDecimal total = calculatedItems.stream()
                .map(item -> item.unitPrice().multiply(BigDecimal.valueOf(item.input().quantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return new CalculatedPricing(calculatedItems, total);
    }

    private void validateRestaurantOwnership(UUID restaurantId, Category category) {
        if (category == null || !Objects.equals(category.getRestaurantId(), restaurantId)) {
            throw new ItemRestaurantMismatchException();
        }
    }

    private void validateAvailability(MenuItem menuItem) {
        if (!menuItem.isAvailable()) {
            throw new ItemUnavailableException();
        }
    }

    private BigDecimal resolveUnitPrice(PricingInput item,
                                        MenuItem menuItem,
                                        Map<MenuComposition.CompositionType, Map<UUID, MenuComposition>> compositionsByType) {
        validateQuantity(item.quantity());

        if (item.menuGroupId() == null) {
            if (item.menuRole() != null) {
                throw new InvalidOrderItemException();
            }
            if (menuItem.isMenuVariant()) {
                throw new InvalidOrderItemException();
            }
            return menuItem.getPrice();
        }

        if (item.menuRole() == null || item.menuRole().isBlank()) {
            throw new InvalidOrderItemException();
        }

        return switch (item.menuRole()) {
            case "plat" -> {
                if (!menuItem.isMenuVariant()) {
                    throw new InvalidOrderItemException();
                }
                yield menuItem.getPrice();
            }
            case "accompagnement" -> resolveSupplementPrice(menuItem.getId(),
                    MenuComposition.CompositionType.accompagnement, compositionsByType);
            case "boisson" -> resolveSupplementPrice(menuItem.getId(),
                    MenuComposition.CompositionType.boisson, compositionsByType);
            default -> throw new InvalidOrderItemException();
        };
    }

    private BigDecimal resolveSupplementPrice(UUID menuItemId,
                                              MenuComposition.CompositionType type,
                                              Map<MenuComposition.CompositionType, Map<UUID, MenuComposition>> compositionsByType) {
        MenuComposition composition = compositionsByType.getOrDefault(type, Map.of()).get(menuItemId);
        if (composition == null) {
            throw new InvalidOrderItemException();
        }
        return composition.getSupplementPrice();
    }

    private Map<MenuComposition.CompositionType, Map<UUID, MenuComposition>> indexCompositions(List<MenuComposition> compositions) {
        Map<MenuComposition.CompositionType, Map<UUID, MenuComposition>> indexed = new EnumMap<>(MenuComposition.CompositionType.class);
        for (MenuComposition composition : compositions) {
            indexed.computeIfAbsent(composition.getCompositionType(), ignored -> new HashMap<>())
                    .put(composition.getMenuItemId(), composition);
        }
        return indexed;
    }

    private void validateComboGroups(List<CalculatedItem> calculatedItems) {
        Map<UUID, List<CalculatedItem>> groupedItems = calculatedItems.stream()
                .filter(item -> item.input().menuGroupId() != null)
                .collect(Collectors.groupingBy(item -> item.input().menuGroupId()));

        for (List<CalculatedItem> group : groupedItems.values()) {
            if (group.size() != 3) {
                throw new InvalidOrderItemException();
            }

            Map<String, Long> roles = group.stream()
                    .collect(Collectors.groupingBy(item -> item.input().menuRole(), Collectors.counting()));

            if (!roles.getOrDefault("plat", 0L).equals(1L)
                    || !roles.getOrDefault("accompagnement", 0L).equals(1L)
                    || !roles.getOrDefault("boisson", 0L).equals(1L)) {
                throw new InvalidOrderItemException();
            }

            long distinctQuantities = group.stream().map(item -> item.input().quantity()).distinct().count();
            if (distinctQuantities != 1) {
                throw new InvalidQuantityException();
            }
        }
    }

    private void validateQuantity(int quantity) {
        if (quantity <= 0) {
            throw new InvalidQuantityException();
        }
    }

    public record RequestItem(UUID menuItemId, int quantity, UUID menuGroupId, String menuRole) {
    }

    public record OrderMenuCatalog(Map<UUID, MenuItem> menuItemsById,
                                   Map<UUID, Category> categoriesById,
                                   List<MenuComposition> compositions) {
    }

    public record PricingResult(List<OrderItem> items, BigDecimal total) {
    }

    private record PricingInput(UUID menuItemId, int quantity, UUID menuGroupId, String menuRole) {
    }

    private record CalculatedItem(PricingInput input, MenuItem menuItem, BigDecimal unitPrice) {
    }

    private record CalculatedPricing(List<CalculatedItem> items, BigDecimal total) {
    }

    public static class OrderItemsNotFoundException extends RuntimeException {
        public OrderItemsNotFoundException() {
            super("Articles de commande introuvables");
        }
    }

    public static class InvalidQuantityException extends IllegalArgumentException {
        public InvalidQuantityException() {
            super("Quantité invalide");
        }
    }

    public static class MenuItemsNotFoundException extends IllegalArgumentException {
        public MenuItemsNotFoundException() {
            super("Articles de menu introuvables");
        }
    }

    public static class ItemRestaurantMismatchException extends RuntimeException {
        public ItemRestaurantMismatchException() {
            super("Article ne correspondant pas au restaurant");
        }
    }

    public static class ItemUnavailableException extends IllegalArgumentException {
        public ItemUnavailableException() {
            super("Un article commandé n'est plus disponible");
        }
    }

    public static class InvalidOrderItemException extends IllegalArgumentException {
        public InvalidOrderItemException() {
            super("Composition de commande invalide");
        }
    }
}
