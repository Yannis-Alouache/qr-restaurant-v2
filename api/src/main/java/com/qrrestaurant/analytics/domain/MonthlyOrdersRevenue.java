package com.qrrestaurant.analytics.domain;

import java.math.BigDecimal;

public record MonthlyOrdersRevenue(int year, int month, long orders, BigDecimal revenue) {}
