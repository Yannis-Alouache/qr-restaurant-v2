package com.qrrestaurant.menu.infrastructure.persistence.category;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CategoryRepositoryAdapter implements CategoryRepository {

    private final CategoryJpaRepository jpaRepo;

    public CategoryRepositoryAdapter(CategoryJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<Category> findByRestaurantIdOrderByPosition(UUID restaurantId) {
        return jpaRepo.findByRestaurantIdOrderByPosition(restaurantId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public Category save(Category category) {
        CategoryJpaEntity entity = toEntity(category);
        CategoryJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }

    private Category toDomain(CategoryJpaEntity e) {
        return Category.from(e.getId(), e.getRestaurantId(), e.getName(),
                e.getImagePath(), e.getPosition(), e.isHasMenu());
    }

    private CategoryJpaEntity toEntity(Category d) {
        CategoryJpaEntity e = new CategoryJpaEntity();
        e.setId(d.getId());
        e.setRestaurantId(d.getRestaurantId());
        e.setName(d.getName());
        e.setImagePath(d.getImagePath());
        e.setPosition(d.getPosition());
        e.setHasMenu(d.isHasMenu());
        return e;
    }
}
