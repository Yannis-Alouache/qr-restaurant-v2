package com.qrrestaurant.restaurant.infrastructure.persistence;

import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryRestaurantTableRepository implements RestaurantTableRepository {
    private final Map<UUID, RestaurantTable> tables = new LinkedHashMap<>();

    @Override
    public RestaurantTable save(RestaurantTable table) {
        UUID id = table.getId() != null ? table.getId() : UUID.randomUUID();
        RestaurantTable saved = new RestaurantTable(id, table.getRestaurantId(), table.getNumber());
        tables.put(id, saved);
        return copy(saved);
    }

    @Override
    public List<RestaurantTable> findByRestaurantIdOrderByNumber(UUID restaurantId) {
        return tables.values().stream()
                .filter(table -> restaurantId.equals(table.getRestaurantId()))
                .sorted(Comparator.comparing(RestaurantTable::getNumber))
                .map(this::copy)
                .toList();
    }

    private RestaurantTable copy(RestaurantTable table) {
        return new RestaurantTable(table.getId(), table.getRestaurantId(), table.getNumber());
    }
}
