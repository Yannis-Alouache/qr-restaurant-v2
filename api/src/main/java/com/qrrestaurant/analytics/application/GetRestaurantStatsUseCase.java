package com.qrrestaurant.analytics.application;

import com.qrrestaurant.analytics.domain.MonthlyOrdersRevenue;
import com.qrrestaurant.analytics.domain.RefundedTotals;
import com.qrrestaurant.analytics.domain.StatsRepository;
import com.qrrestaurant.analytics.domain.TopSellingItem;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetRestaurantStatsUseCase {

    static final int MIN_MONTHS = 1;
    static final int MAX_MONTHS = 24;
    private static final int TOP_ITEMS_LIMIT = 10;

    /**
     * Commandes encaissées : confirmées par le webhook Stripe puis avancées en
     * cuisine. Les paniers jamais payés (en_attente_paiement, paiement_echoue)
     * ne comptent ni dans les commandes ni dans le chiffre d'affaires ; les
     * commandes remboursées sortent du CA et sont comptabilisées à part.
     */
    private static final List<String> PAID_STATUSES = List.of(
            "nouvelle", "en_preparation", "prete", "servie");

    /**
     * created_at est stocké en UTC et agrégé tel quel en SQL : la fenêtre
     * doit donc être tronquée en UTC pour retomber sur les mêmes buckets.
     */
    private static final ZoneOffset STATS_ZONE = ZoneOffset.UTC;

    private final RestaurantRepository restaurantRepository;
    private final StatsRepository statsRepository;

    public GetRestaurantStatsUseCase(RestaurantRepository restaurantRepository,
                                     StatsRepository statsRepository) {
        this.restaurantRepository = restaurantRepository;
        this.statsRepository = statsRepository;
    }

    public RestaurantStatsView getStats(UUID userId, int months) {
        if (months < MIN_MONTHS || months > MAX_MONTHS) {
            throw new InvalidStatsPeriodException();
        }
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);

        YearMonth currentMonth = YearMonth.now(STATS_ZONE);
        YearMonth startMonth = currentMonth.minusMonths(months - 1L);
        Instant since = startMonth.atDay(1).atStartOfDay(STATS_ZONE).toInstant();

        Map<YearMonth, MonthlyOrdersRevenue> paidByMonth = new HashMap<>();
        for (MonthlyOrdersRevenue row : statsRepository.sumPaidOrdersByMonth(restaurant.getId(), PAID_STATUSES, since)) {
            paidByMonth.put(YearMonth.of(row.year(), row.month()), row);
        }

        List<MonthlyStatsView> monthly = new ArrayList<>();
        long totalOrders = 0;
        BigDecimal totalRevenue = BigDecimal.ZERO.setScale(2);
        for (YearMonth month = startMonth; !month.isAfter(currentMonth); month = month.plusMonths(1)) {
            MonthlyOrdersRevenue row = paidByMonth.get(month);
            long orders = row != null ? row.orders() : 0;
            BigDecimal revenue = row != null ? row.revenue() : BigDecimal.ZERO.setScale(2);
            totalOrders += orders;
            totalRevenue = totalRevenue.add(revenue);
            monthly.add(new MonthlyStatsView(month.toString(), orders, revenue, average(revenue, orders)));
        }

        RefundedTotals refunded = statsRepository.sumRefundedOrders(restaurant.getId(), since);
        List<TopItemStatsView> topItems = statsRepository
                .findTopSellingItems(restaurant.getId(), PAID_STATUSES, since, TOP_ITEMS_LIMIT)
                .stream()
                .map(item -> new TopItemStatsView(item.menuItemId().toString(), item.name(),
                        item.quantitySold(), item.revenue()))
                .toList();

        return new RestaurantStatsView(
                new PeriodView(startMonth.atDay(1).toString(), currentMonth.atEndOfMonth().toString()),
                new SummaryView(totalOrders, totalRevenue, average(totalRevenue, totalOrders),
                        refunded.orders(), refunded.amount()),
                List.copyOf(monthly),
                topItems);
    }

    private static BigDecimal average(BigDecimal revenue, long orders) {
        if (orders == 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return revenue.divide(BigDecimal.valueOf(orders), 2, RoundingMode.HALF_UP);
    }

    public record RestaurantStatsView(PeriodView period, SummaryView summary,
                                      List<MonthlyStatsView> monthly, List<TopItemStatsView> topItems) {}
    public record PeriodView(String start, String end) {}
    public record SummaryView(long totalOrders, BigDecimal totalRevenue, BigDecimal averageOrderValue,
                              long refundedOrders, BigDecimal refundedAmount) {}
    public record MonthlyStatsView(String month, long orders, BigDecimal revenue, BigDecimal averageOrderValue) {}
    public record TopItemStatsView(String menuItemId, String name, long quantitySold, BigDecimal revenue) {}

    public static class InvalidStatsPeriodException extends RuntimeException {
        public InvalidStatsPeriodException() { super("Le nombre de mois doit être compris entre 1 et 24"); }
    }

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant trouvé"); }
    }
}
