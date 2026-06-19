package com.qrrestaurant.menu.infrastructure.persistence.composition;

import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuCompositionRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class InMemoryMenuCompositionRepository implements MenuCompositionRepository {
    private final Map<UUID, MenuComposition> compositions = new LinkedHashMap<>();

    @Override
    public MenuComposition save(MenuComposition composition) {
        UUID id = composition.getId() != null ? composition.getId() : UUID.randomUUID();
        MenuComposition saved = MenuComposition.from(id, composition.getRestaurantId(), composition.getCompositionType(),
                composition.getMenuItemId(), composition.getSupplementPrice());
        compositions.put(id, saved);
        return copy(saved);
    }

    @Override
    public List<MenuComposition> findByRestaurantId(UUID restaurantId) {
        return compositions.values().stream()
                .filter(composition -> restaurantId.equals(composition.getRestaurantId()))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<MenuComposition> findByRestaurantIdAndCompositionType(UUID restaurantId, MenuComposition.CompositionType type) {
        return compositions.values().stream()
                .filter(composition -> restaurantId.equals(composition.getRestaurantId()))
                .filter(composition -> type == composition.getCompositionType())
                .map(this::copy)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        compositions.remove(id);
    }

    private MenuComposition copy(MenuComposition composition) {
        return MenuComposition.from(composition.getId(), composition.getRestaurantId(), composition.getCompositionType(),
                composition.getMenuItemId(), composition.getSupplementPrice());
    }
}
