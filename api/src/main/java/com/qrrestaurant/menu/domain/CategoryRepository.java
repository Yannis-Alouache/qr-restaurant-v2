package com.qrrestaurant.menu.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {
    Category save(Category category);
    Optional<Category> findById(UUID id);
    List<Category> findByRestaurantIdOrderByPosition(UUID restaurantId);
    void deleteById(UUID id);
}
