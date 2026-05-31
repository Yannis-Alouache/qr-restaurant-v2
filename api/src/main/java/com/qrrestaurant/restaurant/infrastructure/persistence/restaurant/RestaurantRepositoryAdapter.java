package com.qrrestaurant.restaurant.infrastructure.persistence.restaurant;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public class RestaurantRepositoryAdapter implements RestaurantRepository {

    private final RestaurantJpaRepository jpaRepo;

    public RestaurantRepositoryAdapter(RestaurantJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public Restaurant save(Restaurant restaurant) {
        RestaurantJpaEntity entity = toEntity(restaurant);
        RestaurantJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Restaurant> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public Optional<Restaurant> findBySlug(String slug) {
        return jpaRepo.findBySlug(slug).map(this::toDomain);
    }

    @Override
    public Optional<Restaurant> findByUserId(UUID userId) {
        return jpaRepo.findByUserId(userId).map(this::toDomain);
    }

    @Override
    public boolean existsBySlug(String slug) {
        return jpaRepo.existsBySlug(slug);
    }

    private Restaurant toDomain(RestaurantJpaEntity e) {
        return new Restaurant(
                e.getId(), e.getUserId(), e.getName(), e.getSlug(),
                e.getAddress(), e.getLogoPath(), e.getThemeId(),
                e.getPaymentProviderAccountId(), e.getCreatedAt()
        );
    }

    private RestaurantJpaEntity toEntity(Restaurant d) {
        RestaurantJpaEntity e = new RestaurantJpaEntity();
        e.setId(d.getId());
        e.setUserId(d.getUserId());
        e.setName(d.getName());
        e.setSlug(d.getSlug());
        e.setAddress(d.getAddress());
        e.setLogoPath(d.getLogoPath());
        e.setThemeId(d.getThemeId());
        e.setPaymentProviderAccountId(d.getPaymentProviderAccountId());
        return e;
    }
}
