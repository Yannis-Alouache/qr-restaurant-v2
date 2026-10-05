package com.qrrestaurant.analytics.application;

import com.qrrestaurant.analytics.domain.MonthlyOrdersRevenue;
import com.qrrestaurant.analytics.domain.RefundedTotals;
import com.qrrestaurant.analytics.domain.StatsRepository;
import com.qrrestaurant.analytics.domain.TopSellingItem;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GetRestaurantStatsUseCaseTest {

    private static final UUID OWNER_ID = UUID.randomUUID();
    private static final UUID RESTAURANT_ID = UUID.randomUUID();

    private final InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
    private final FakeStatsRepository statsRepository = new FakeStatsRepository();
    private final GetRestaurantStatsUseCase useCase = new GetRestaurantStatsUseCase(restaurantRepository, statsRepository);

    @BeforeEach
    void seedRestaurant() {
        restaurantRepository.save(Restaurant.from(RESTAURANT_ID, OWNER_ID, "Naia Burger", "naia-burger",
                null, null, null, "chaud", null, LocalDateTime.now()));
    }

    @Test
    void shouldRejectWindowOutsideAllowedBounds() {
        assertThrows(GetRestaurantStatsUseCase.InvalidStatsPeriodException.class, () -> useCase.getStats(OWNER_ID, 0));
        assertThrows(GetRestaurantStatsUseCase.InvalidStatsPeriodException.class, () -> useCase.getStats(OWNER_ID, 25));
        assertThrows(GetRestaurantStatsUseCase.InvalidStatsPeriodException.class,
                () -> useCase.getStats(UUID.randomUUID(), 0));
    }

    @Test
    void shouldRejectOwnerWithoutRestaurant() {
        assertThrows(GetRestaurantStatsUseCase.NoRestaurantException.class,
                () -> useCase.getStats(UUID.randomUUID(), 12));
    }

    @Test
    void shouldZeroFillEveryMonthBetweenWindowBounds() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        statsRepository.monthlyRows = List.of(
                new MonthlyOrdersRevenue(currentMonth.getYear(), currentMonth.getMonthValue(), 2, money("30.00")));

        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 12);

        assertEquals(12, view.monthly().size());
        assertEquals(currentMonth.minusMonths(11).toString(), view.monthly().get(0).month());
        assertEquals(currentMonth.toString(), view.monthly().get(11).month());
        GetRestaurantStatsUseCase.MonthlyStatsView emptyMonth = view.monthly().get(5);
        assertEquals(0, emptyMonth.orders());
        assertMoney(emptyMonth.revenue(), "0.00");
        assertMoney(emptyMonth.averageOrderValue(), "0.00");
        assertMoney(view.summary().totalRevenue(), "30.00");
        assertEquals(2, view.summary().totalOrders());
    }

    @Test
    void shouldRestrictWindowToOneMonthWhenAsked() {
        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 1);

        assertEquals(1, view.monthly().size());
        assertEquals(YearMonth.now(ZoneOffset.UTC).toString(), view.monthly().get(0).month());
        assertEquals(YearMonth.now(ZoneOffset.UTC).atDay(1).toString(), view.period().start());
    }

    @Test
    void shouldAggregateSummaryAcrossMonthlyRows() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        YearMonth previousMonth = currentMonth.minusMonths(1);
        statsRepository.monthlyRows = List.of(
                new MonthlyOrdersRevenue(currentMonth.getYear(), currentMonth.getMonthValue(), 2, money("30.00")),
                new MonthlyOrdersRevenue(previousMonth.getYear(), previousMonth.getMonthValue(), 3, money("20.00")));

        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 12);

        assertEquals(5, view.summary().totalOrders());
        assertMoney(view.summary().totalRevenue(), "50.00");
        assertMoney(view.summary().averageOrderValue(), "10.00");
    }

    @Test
    void shouldRoundAverageOrderValueHalfUpOnTwoDecimals() {
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);
        statsRepository.monthlyRows = List.of(
                new MonthlyOrdersRevenue(currentMonth.getYear(), currentMonth.getMonthValue(), 3, money("50.00")));

        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 12);

        assertMoney(view.summary().averageOrderValue(), "16.67");
    }

    @Test
    void shouldReportRefundedOrdersSeparatelyFromRevenue() {
        statsRepository.refundedTotals = new RefundedTotals(1, money("45.20"));

        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 12);

        assertEquals(1, view.summary().refundedOrders());
        assertMoney(view.summary().refundedAmount(), "45.20");
        assertMoney(view.summary().totalRevenue(), "0.00");
    }

    @Test
    void shouldMapTopSellingItems() {
        UUID menuItemId = UUID.randomUUID();
        statsRepository.topItemRows = List.of(new TopSellingItem(menuItemId, "Frites", 5, money("17.50")));

        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 12);

        assertEquals(1, view.topItems().size());
        assertEquals(menuItemId.toString(), view.topItems().get(0).menuItemId());
        assertEquals("Frites", view.topItems().get(0).name());
        assertEquals(5, view.topItems().get(0).quantitySold());
        assertMoney(view.topItems().get(0).revenue(), "17.50");
    }

    @Test
    void shouldReturnAllZerosWhenRestaurantHasNoOrder() {
        GetRestaurantStatsUseCase.RestaurantStatsView view = useCase.getStats(OWNER_ID, 6);

        assertEquals(6, view.monthly().size());
        assertEquals(0, view.summary().totalOrders());
        assertMoney(view.summary().totalRevenue(), "0.00");
        assertMoney(view.summary().refundedAmount(), "0.00");
        assertEquals(0, view.topItems().size());
    }

    private static BigDecimal money(String value) {
        return new BigDecimal(value);
    }

    private static void assertMoney(BigDecimal actual, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(actual),
                "montant attendu " + expected + " mais lu " + actual.toPlainString());
    }

    private static final class FakeStatsRepository implements StatsRepository {
        private List<MonthlyOrdersRevenue> monthlyRows = List.of();
        private RefundedTotals refundedTotals = new RefundedTotals(0, BigDecimal.ZERO.setScale(2));
        private List<TopSellingItem> topItemRows = List.of();

        @Override
        public List<MonthlyOrdersRevenue> sumPaidOrdersByMonth(UUID restaurantId, Collection<String> paidStatuses, Instant since) {
            return monthlyRows;
        }

        @Override
        public RefundedTotals sumRefundedOrders(UUID restaurantId, Instant since) {
            return refundedTotals;
        }

        @Override
        public List<TopSellingItem> findTopSellingItems(UUID restaurantId, Collection<String> paidStatuses, Instant since, int limit) {
            return topItemRows;
        }
    }
}
