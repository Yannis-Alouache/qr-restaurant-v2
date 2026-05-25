package com.qrrestaurant.menu.application;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record MenuView(
    RestaurantInfo restaurant,
    List<CategoryView> categories,
    List<CompositionEntry> compositions
) {
    public record RestaurantInfo(
        UUID id,
        String name,
        String slug,
        String address,
        String logoPath,
        String themeId
    ) {}

    public record CategoryView(
        UUID id,
        String name,
        String imagePath,
        int position,
        boolean hasMenu,
        List<ItemView> items
    ) {}

    public record ItemView(
        UUID id,
        String name,
        String description,
        BigDecimal price,
        String imagePath,
        boolean available,
        UUID menuVariantOf
    ) {}

    public record CompositionEntry(
        UUID id,
        String compositionType,
        UUID menuItemId,
        String menuItemName,
        String menuItemImagePath,
        BigDecimal supplementPrice
    ) {}
}
