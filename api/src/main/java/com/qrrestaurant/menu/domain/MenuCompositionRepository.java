package com.qrrestaurant.menu.domain;

import java.util.List;
import java.util.UUID;

public interface MenuCompositionRepository {
    MenuComposition save(MenuComposition composition);
    List<MenuComposition> findByRestaurantId(UUID restaurantId);
    List<MenuComposition> findByRestaurantIdAndCompositionType(UUID restaurantId, MenuComposition.CompositionType type);
    void deleteById(UUID id);
}
