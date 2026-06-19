package com.qrrestaurant.menu.infrastructure.persistence.category;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryCategoryRepository implements CategoryRepository {
    private final Map<UUID, Category> categories = new LinkedHashMap<>();

    @Override
    public Category save(Category category) {
        UUID id = category.getId();
        Category saved = Category.from(id, category.getRestaurantId(), category.getName(), category.getImagePath(),
                category.getPosition(), category.isHasMenu());
        categories.put(id, saved);
        return copy(saved);
    }

    @Override
    public Optional<Category> findById(UUID id) {
        return Optional.ofNullable(categories.get(id)).map(this::copy);
    }

    @Override
    public List<Category> findByRestaurantIdOrderByPosition(UUID restaurantId) {
        return categories.values().stream()
                .filter(category -> restaurantId.equals(category.getRestaurantId()))
                .sorted(Comparator.comparing(Category::getPosition))
                .map(this::copy)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        categories.remove(id);
    }

    private Category copy(Category category) {
        return Category.from(category.getId(), category.getRestaurantId(), category.getName(), category.getImagePath(),
                category.getPosition(), category.isHasMenu());
    }
}
