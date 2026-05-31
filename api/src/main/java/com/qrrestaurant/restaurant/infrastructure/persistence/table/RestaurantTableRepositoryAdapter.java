package com.qrrestaurant.restaurant.infrastructure.persistence.table;

import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class RestaurantTableRepositoryAdapter implements RestaurantTableRepository {

    private final RestaurantTableJpaRepository jpaRepo;

    public RestaurantTableRepositoryAdapter(RestaurantTableJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public RestaurantTable save(RestaurantTable table) {
        RestaurantTableJpaEntity entity = toEntity(table);
        RestaurantTableJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<RestaurantTable> findByRestaurantIdOrderByNumber(UUID restaurantId) {
        return jpaRepo.findByRestaurantIdOrderByNumber(restaurantId).stream()
                .map(this::toDomain).toList();
    }

    private RestaurantTable toDomain(RestaurantTableJpaEntity e) {
        return new RestaurantTable(e.getId(), e.getRestaurantId(), e.getNumber());
    }

    private RestaurantTableJpaEntity toEntity(RestaurantTable d) {
        RestaurantTableJpaEntity e = new RestaurantTableJpaEntity();
        e.setId(d.getId());
        e.setRestaurantId(d.getRestaurantId());
        e.setNumber(d.getNumber());
        return e;
    }
}
