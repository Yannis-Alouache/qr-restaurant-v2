package com.qrrestaurant.analytics.domain;

import java.math.BigDecimal;
import java.util.UUID;

public record TopSellingItem(UUID menuItemId, String name, long quantitySold, BigDecimal revenue) {}
