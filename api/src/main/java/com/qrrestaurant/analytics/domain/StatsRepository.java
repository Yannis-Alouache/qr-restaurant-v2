package com.qrrestaurant.analytics.domain;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface StatsRepository {
    List<MonthlyOrdersRevenue> sumPaidOrdersByMonth(UUID restaurantId, Collection<String> paidStatuses, Instant since);

    RefundedTotals sumRefundedOrders(UUID restaurantId, Instant since);

    List<TopSellingItem> findTopSellingItems(UUID restaurantId, Collection<String> paidStatuses, Instant since, int limit);
}
