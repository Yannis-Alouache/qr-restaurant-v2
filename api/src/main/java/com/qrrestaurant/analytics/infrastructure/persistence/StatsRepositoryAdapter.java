package com.qrrestaurant.analytics.infrastructure.persistence;

import com.qrrestaurant.analytics.domain.MonthlyOrdersRevenue;
import com.qrrestaurant.analytics.domain.RefundedTotals;
import com.qrrestaurant.analytics.domain.StatsRepository;
import com.qrrestaurant.analytics.domain.TopSellingItem;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Agrégats statistiques lus directement en SQL : aucune entité JPA ne porte
 * ces vues de lecture, et le graphe order_item -> order_table n'existe pas
 * côté entités. Postgres uniquement (EXTRACT, LIMIT paramétré).
 */
@Repository
public class StatsRepositoryAdapter implements StatsRepository {

    private static final String MONTHLY_PAID_SQL = """
            SELECT EXTRACT(YEAR FROM created_at) AS yr,
                   EXTRACT(MONTH FROM created_at) AS mth,
                   COUNT(*) AS order_count,
                   COALESCE(SUM(total), 0) AS revenue
            FROM order_table
            WHERE restaurant_id = :restaurantId
              AND status IN (:statuses)
              AND created_at >= :since
            GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at)
            ORDER BY 1, 2
            """;

    private static final String REFUNDED_SQL = """
            SELECT COUNT(*) AS order_count, COALESCE(SUM(total), 0) AS amount
            FROM order_table
            WHERE restaurant_id = :restaurantId
              AND status = 'rembourse'
              AND created_at >= :since
            """;

    private static final String TOP_ITEMS_SQL = """
            SELECT oi.menu_item_id,
                   oi.name,
                   SUM(oi.quantity) AS quantity_sold,
                   SUM(oi.quantity * oi.unit_price) AS revenue
            FROM order_item oi
            JOIN order_table o ON o.id = oi.order_id
            WHERE o.restaurant_id = :restaurantId
              AND o.status IN (:statuses)
              AND o.created_at >= :since
            GROUP BY oi.menu_item_id, oi.name
            ORDER BY revenue DESC
            LIMIT :limit
            """;

    private final NamedParameterJdbcTemplate jdbc;

    public StatsRepositoryAdapter(NamedParameterJdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public List<MonthlyOrdersRevenue> sumPaidOrdersByMonth(UUID restaurantId, Collection<String> statuses, Instant since) {
        return jdbc.query(MONTHLY_PAID_SQL, params(restaurantId, statuses, since), (rs, rowNum) ->
                new MonthlyOrdersRevenue(rs.getInt("yr"), rs.getInt("mth"),
                        rs.getLong("order_count"), rs.getBigDecimal("revenue")));
    }

    @Override
    public RefundedTotals sumRefundedOrders(UUID restaurantId, Instant since) {
        RefundedTotals totals = jdbc.queryForObject(REFUNDED_SQL, params(restaurantId, List.of(), since), (rs, rowNum) ->
                new RefundedTotals(rs.getLong("order_count"), rs.getBigDecimal("amount")));
        return totals != null ? totals : new RefundedTotals(0, java.math.BigDecimal.ZERO);
    }

    @Override
    public List<TopSellingItem> findTopSellingItems(UUID restaurantId, Collection<String> statuses, Instant since, int limit) {
        return jdbc.query(TOP_ITEMS_SQL,
                params(restaurantId, statuses, since).addValue("limit", limit), (rs, rowNum) ->
                new TopSellingItem(rs.getObject("menu_item_id", UUID.class), rs.getString("name"),
                        rs.getLong("quantity_sold"), rs.getBigDecimal("revenue")));
    }

    private MapSqlParameterSource params(UUID restaurantId, Collection<String> statuses, Instant since) {
        return new MapSqlParameterSource()
                .addValue("restaurantId", restaurantId)
                .addValue("statuses", statuses)
                .addValue("since", Timestamp.from(since));
    }
}
