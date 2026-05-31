package com.qrrestaurant.menu.application.dto;

import java.math.BigDecimal;

public record CompositionView(
        String id,
        String compositionType,
        String menuItemId,
        BigDecimal supplementPrice
) {}
