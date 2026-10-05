package com.qrrestaurant.analytics.presentation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrrestaurant.auth.infrastructure.security.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import com.qrrestaurant.support.TestAuthCookies;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.sql.Timestamp;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class StatsControllerHttpTest extends AbstractPostgresIntegrationTest {

    private static final UUID BURGERS_CATEGORY_ID = UUID.fromString("d0eebc99-9c0b-4ef8-bb6d-6bb9bd380a01");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    @AfterEach
    void cleanAnalyticsData() {
        jdbcTemplate.update("DELETE FROM order_item");
        jdbcTemplate.update("DELETE FROM order_table");
        jdbcTemplate.update("DELETE FROM menu_item WHERE name LIKE 'Article %'");
        jdbcTemplate.update("DELETE FROM app_user WHERE id <> ?", OWNER_ID);
    }

    @Test
    void shouldBuildMonthlySeriesOverTwelveMonthsByDefault() throws Exception {
        insertOrder("nouvelle", new BigDecimal("12.00"), timestampInMonth(3, 10));
        insertOrder("servie", new BigDecimal("8.00"), timestampInMonth(3, 12));
        insertOrder("nouvelle", new BigDecimal("15.50"), timestampInMonth(0, 2));
        insertOrder("rembourse", new BigDecimal("30.00"), timestampInMonth(0, 3));
        insertOrder("en_attente_paiement", new BigDecimal("99.99"), timestampInMonth(0, 4));
        insertOrder("paiement_echoue", new BigDecimal("70.00"), timestampInMonth(0, 5));

        MvcResult result = mockMvc.perform(get("/api/admin/stats")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stats = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);

        assertEquals(currentMonth.minusMonths(11).atDay(1).toString(), stats.path("period").path("start").asText());
        assertEquals(currentMonth.atEndOfMonth().toString(), stats.path("period").path("end").asText());

        JsonNode monthly = stats.path("monthly");
        assertEquals(12, monthly.size());
        assertEquals(currentMonth.minusMonths(11).toString(), monthly.get(0).path("month").asText());
        assertEquals(currentMonth.toString(), monthly.get(11).path("month").asText());

        JsonNode threeMonthsAgo = bucket(monthly, currentMonth.minusMonths(3));
        assertEquals(2, threeMonthsAgo.path("orders").asLong());
        assertMoney(threeMonthsAgo.path("revenue"), "20.00");

        JsonNode current = bucket(monthly, currentMonth);
        assertEquals(1, current.path("orders").asLong());
        assertMoney(current.path("revenue"), "15.50");

        JsonNode emptyMonth = bucket(monthly, currentMonth.minusMonths(1));
        assertEquals(0, emptyMonth.path("orders").asLong());
        assertMoney(emptyMonth.path("revenue"), "0.00");
        assertMoney(emptyMonth.path("averageOrderValue"), "0.00");

        JsonNode summary = stats.path("summary");
        assertEquals(3, summary.path("totalOrders").asLong());
        assertMoney(summary.path("totalRevenue"), "35.50");
        assertMoney(summary.path("averageOrderValue"), "11.83");
        assertEquals(1, summary.path("refundedOrders").asLong());
        assertMoney(summary.path("refundedAmount"), "30.00");
    }

    @Test
    void shouldRankTopSellingItemsFromPaidOrdersOnly() throws Exception {
        UUID firstOrder = insertOrder("nouvelle", new BigDecimal("55.00"), timestampInMonth(0, 2));
        UUID secondOrder = insertOrder("servie", new BigDecimal("40.00"), timestampInMonth(0, 3));
        UUID unpaidCart = insertOrder("en_attente_paiement", new BigDecimal("45.00"), timestampInMonth(0, 4));
        insertItem(firstOrder, BACON_MENU_ID, "Menu Burger bacon", 3, "8.50");
        insertItem(firstOrder, FRIES_ID, "Frites", 1, "3.50");
        insertItem(firstOrder, COKE_ID, "Coca-Cola", 2, "2.50");
        insertItem(secondOrder, BACON_MENU_ID, "Menu Burger bacon", 1, "8.50");
        insertItem(secondOrder, FRIES_ID, "Frites", 4, "3.50");
        insertItem(unpaidCart, BROWNIE_ID, "Brownie maison", 10, "4.50");

        MvcResult result = mockMvc.perform(get("/api/admin/stats")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode topItems = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("topItems");

        assertEquals(3, topItems.size());
        assertEquals("Menu Burger bacon", topItems.get(0).path("name").asText());
        assertEquals(4, topItems.get(0).path("quantitySold").asLong());
        assertMoney(topItems.get(0).path("revenue"), "34.00");
        assertEquals("Frites", topItems.get(1).path("name").asText());
        assertEquals(5, topItems.get(1).path("quantitySold").asLong());
        assertMoney(topItems.get(1).path("revenue"), "17.50");
        assertEquals("Coca-Cola", topItems.get(2).path("name").asText());
        assertEquals(2, topItems.get(2).path("quantitySold").asLong());
        assertMoney(topItems.get(2).path("revenue"), "5.00");
    }

    @Test
    void shouldLimitTopSellingItemsToTen() throws Exception {
        UUID orderId = insertOrder("nouvelle", new BigDecimal("100.00"), timestampInMonth(0, 2));
        for (int i = 0; i < 11; i++) {
            UUID menuItemId = UUID.randomUUID();
            jdbcTemplate.update(
                    """
                    INSERT INTO menu_item (id, category_id, name, description, price, available)
                    VALUES (?, ?, ?, ?, ?, ?)
                    """,
                    menuItemId,
                    BURGERS_CATEGORY_ID,
                    "Article " + i,
                    "Fixture stats",
                    new BigDecimal(10 + i),
                    true);
            insertItem(orderId, menuItemId, "Article " + i, 1, new BigDecimal(10 + i));
        }

        MvcResult result = mockMvc.perform(get("/api/admin/stats")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode topItems = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString())
                .path("topItems");
        assertEquals(10, topItems.size());
        for (JsonNode item : topItems) {
            assertTrue(!"Article 0".equals(item.path("name").asText()),
                    "L'article le moins vendu doit être évincé du top 10");
        }
    }

    @Test
    void shouldRoundAverageOrderValueHalfUpOnTwoDecimals() throws Exception {
        insertOrder("nouvelle", new BigDecimal("16.67"), timestampInMonth(0, 2));
        insertOrder("nouvelle", new BigDecimal("16.67"), timestampInMonth(0, 3));
        insertOrder("nouvelle", new BigDecimal("16.66"), timestampInMonth(0, 4));

        mockMvc.perform(get("/api/admin/stats")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary.totalOrders").value(3))
                .andExpect(jsonPath("$.summary.totalRevenue").value(50.00))
                .andExpect(jsonPath("$.summary.averageOrderValue").value(16.67));
    }

    @Test
    void shouldRestrictWindowWhenMonthsParamProvided() throws Exception {
        insertOrder("nouvelle", new BigDecimal("20.00"), timestampInMonth(0, 2));
        insertOrder("nouvelle", new BigDecimal("10.00"), timestampInMonth(1, 10));

        MvcResult result = mockMvc.perform(get("/api/admin/stats?months=1")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode stats = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        assertEquals(1, stats.path("monthly").size());
        assertEquals(YearMonth.now(ZoneOffset.UTC).toString(),
                stats.path("monthly").get(0).path("month").asText());
        assertEquals(1, stats.path("summary").path("totalOrders").asLong());
        assertMoney(stats.path("summary").path("totalRevenue"), "20.00");
    }

    @Test
    void shouldRejectOutOfRangeWindows() throws Exception {
        mockMvc.perform(get("/api/admin/stats?months=0")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le nombre de mois doit être compris entre 1 et 24"));

        mockMvc.perform(get("/api/admin/stats?months=25")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le nombre de mois doit être compris entre 1 et 24"));
    }

    @Test
    void shouldRejectAnonymousCallers() throws Exception {
        mockMvc.perform(get("/api/admin/stats"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void shouldReturn404WhenUserOwnsNoRestaurant() throws Exception {
        UUID stranger = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                stranger, "stranger-stats@test.com", "no-login");

        mockMvc.perform(get("/api/admin/stats")
                        .cookie(TestAuthCookies.jwt(jwtService, stranger, "stranger-stats@test.com")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun restaurant trouvé"));
    }

    @Test
    void shouldIsolateStatsPerRestaurant() throws Exception {
        insertOrder("nouvelle", new BigDecimal("24.00"), timestampInMonth(0, 2));

        UUID otherOwner = UUID.randomUUID();
        UUID otherRestaurant = UUID.randomUUID();
        UUID otherTable = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                otherOwner, "marco-stats@test.com", "no-login");
        jdbcTemplate.update("""
                INSERT INTO restaurant (id, user_id, name, slug, theme_id)
                VALUES (?, ?, ?, ?, ?)
                """, otherRestaurant, otherOwner, "Chez Marco", "chez-marco-stats", "chaud");
        jdbcTemplate.update("INSERT INTO restaurant_table (id, restaurant_id, number) VALUES (?, ?, ?)",
                otherTable, otherRestaurant, 1);
        insertOrder(otherRestaurant, otherTable, "nouvelle", new BigDecimal("999.99"), timestampInMonth(0, 3));

        MvcResult result = mockMvc.perform(get("/api/admin/stats")
                        .cookie(ownerBearerToken()))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode ownerStats = new ObjectMapper()
                .readTree(result.getResponse().getContentAsString());
        assertEquals(1, ownerStats.path("summary").path("totalOrders").asLong());
        assertMoney(ownerStats.path("summary").path("totalRevenue"), "24.00");

        MvcResult otherResult = mockMvc.perform(get("/api/admin/stats")
                        .cookie(TestAuthCookies.jwt(jwtService, otherOwner, "marco-stats@test.com")))
                .andExpect(status().isOk())
                .andReturn();
        JsonNode otherStats = new ObjectMapper()
                .readTree(otherResult.getResponse().getContentAsString());
        assertEquals(1, otherStats.path("summary").path("totalOrders").asLong());
        assertMoney(otherStats.path("summary").path("totalRevenue"), "999.99");
    }

    private UUID insertOrder(String status, BigDecimal total, Timestamp createdAt) {
        return insertOrder(RESTAURANT_ID, TABLE_1_ID, status, total, createdAt);
    }

    private UUID insertOrder(UUID restaurantId, UUID tableId, String status, BigDecimal total, Timestamp createdAt) {
        UUID orderId = UUID.randomUUID();
        jdbcTemplate.update(
                """
                INSERT INTO order_table (id, restaurant_id, table_id, status, total, payment_transaction_id, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """,
                orderId,
                restaurantId,
                tableId,
                status,
                total,
                "pi_" + orderId,
                createdAt);
        return orderId;
    }

    private void insertItem(UUID orderId, UUID menuItemId, String name, int quantity, String unitPrice) {
        insertItem(orderId, menuItemId, name, quantity, new BigDecimal(unitPrice));
    }

    private void insertItem(UUID orderId, UUID menuItemId, String name, int quantity, BigDecimal unitPrice) {
        jdbcTemplate.update(
                """
                INSERT INTO order_item (id, order_id, menu_item_id, name, quantity, unit_price)
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                UUID.randomUUID(),
                orderId,
                menuItemId,
                name,
                quantity,
                unitPrice);
    }

    private static Timestamp timestampInMonth(long monthsAgo, int dayOfMonth) {
        YearMonth month = YearMonth.now(ZoneOffset.UTC).minusMonths(monthsAgo);
        return Timestamp.from(month.atDay(dayOfMonth).atTime(12, 0).toInstant(ZoneOffset.UTC));
    }

    private static JsonNode bucket(JsonNode monthly, YearMonth month) {
        for (JsonNode entry : monthly) {
            if (month.toString().equals(entry.path("month").asText())) {
                return entry;
            }
        }
        throw new AssertionError("Mois " + month + " absent de la série mensuelle");
    }

    private static void assertMoney(JsonNode node, String expected) {
        assertEquals(0, new BigDecimal(expected).compareTo(node.decimalValue()),
                "montant attendu " + expected + " mais lu " + node.asText());
    }

    private Cookie ownerBearerToken() {
        return TestAuthCookies.jwt(jwtService, OWNER_ID, "owner@test.com");
    }
}
