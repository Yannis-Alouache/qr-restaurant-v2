package com.qrrestaurant.menu.infrastructure.persistence;

import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuCompositionRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public class MenuCompositionRepositoryAdapter implements MenuCompositionRepository {

    private final MenuCompositionJpaRepository jpaRepo;

    public MenuCompositionRepositoryAdapter(MenuCompositionJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public MenuComposition save(MenuComposition composition) {
        MenuCompositionJpaEntity entity = toEntity(composition);
        MenuCompositionJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public List<MenuComposition> findByRestaurantId(UUID restaurantId) {
        return jpaRepo.findByRestaurantId(restaurantId).stream().map(this::toDomain).toList();
    }

    @Override
    public List<MenuComposition> findByRestaurantIdAndCompositionType(UUID restaurantId, MenuComposition.CompositionType type) {
        return jpaRepo.findByRestaurantIdAndCompositionType(restaurantId, type.name())
                .stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }

    private MenuComposition toDomain(MenuCompositionJpaEntity e) {
        return new MenuComposition(e.getId(), e.getRestaurantId(),
                MenuComposition.CompositionType.valueOf(e.getCompositionType()),
                e.getMenuItemId(), e.getSupplementPrice());
    }

    private MenuCompositionJpaEntity toEntity(MenuComposition d) {
        MenuCompositionJpaEntity e = new MenuCompositionJpaEntity();
        e.setId(d.getId());
        e.setRestaurantId(d.getRestaurantId());
        e.setCompositionType(d.getCompositionType().name());
        e.setMenuItemId(d.getMenuItemId());
        e.setSupplementPrice(d.getSupplementPrice());
        return e;
    }
}
