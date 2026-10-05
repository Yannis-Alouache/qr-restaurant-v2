package com.qrrestaurant.analytics.domain;

import java.math.BigDecimal;

public record RefundedTotals(long orders, BigDecimal amount) {}
