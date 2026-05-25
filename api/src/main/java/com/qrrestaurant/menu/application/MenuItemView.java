package com.qrrestaurant.menu.application;

import java.math.BigDecimal;

public record MenuItemView(
        String id,
        String categoryId,
        String name,
        String description,
        BigDecimal price,
        String imagePath,
        boolean available,
        String menuVariantOf
) {}
