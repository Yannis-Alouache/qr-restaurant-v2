package com.qrrestaurant.restaurant.infrastructure.persistence;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Transactional
class RestaurantPersistenceIntegrationTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private RestaurantRepository restaurantRepository;
    @Autowired
    private RestaurantTableRepository restaurantTableRepository;
    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldPersistAndReloadRestaurantQueriesAndTablesWithPostgres() {
        UUID ownerId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                ownerId,
                "bistro-verde-owner@test.com",
                "encoded-password");

        Restaurant restaurant = Restaurant.from(null, ownerId, "Bistro Verde", "bistro-verde",
                "10 rue des Jardins", null, "classique", "acct_bistro_verde", null);
        Restaurant savedRestaurant = restaurantRepository.save(restaurant);

        restaurantTableRepository.save(RestaurantTable.create(savedRestaurant.getId(), 8));
        restaurantTableRepository.save(RestaurantTable.create(savedRestaurant.getId(), 3));

        Restaurant bySlug = restaurantRepository.findBySlug("bistro-verde").orElseThrow();
        Restaurant byUserId = restaurantRepository.findByUserId(ownerId).orElseThrow();
        List<RestaurantTable> tables = restaurantTableRepository.findByRestaurantIdOrderByNumber(savedRestaurant.getId());

        assertEquals(savedRestaurant.getId(), bySlug.getId());
        assertEquals(savedRestaurant.getId(), byUserId.getId());
        assertTrue(restaurantRepository.existsBySlug("bistro-verde"));
        assertFalse(restaurantRepository.existsBySlug("unknown-slug"));
        assertEquals(List.of(3, 8), tables.stream().map(RestaurantTable::getNumber).toList());
    }

    @Test
    void shouldRejectSavingTwoRestaurantsForTheSameOwner() {
        UUID ownerId = UUID.randomUUID();
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                ownerId,
                "owner-one-restaurant@test.com",
                "encoded-password");

        jdbcTemplate.update(
                "INSERT INTO restaurant (user_id, name, slug, theme_id) VALUES (?, ?, ?, ?)",
                ownerId,
                "Premier",
                "premier",
                "classique");

        assertThrows(DataIntegrityViolationException.class, () -> jdbcTemplate.update(
                "INSERT INTO restaurant (user_id, name, slug, theme_id) VALUES (?, ?, ?, ?)",
                ownerId,
                "Second",
                "second",
                "chaud"));
    }
}
