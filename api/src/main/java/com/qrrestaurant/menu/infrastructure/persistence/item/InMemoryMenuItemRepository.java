package com.qrrestaurant.menu.infrastructure.persistence.item;

import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryMenuItemRepository implements MenuItemRepository {
    private final Map<UUID, MenuItem> items = new LinkedHashMap<>();

    @Override
    public MenuItem save(MenuItem item) {
        UUID id = item.getId();
        MenuItem saved = MenuItem.from(id, item.getCategoryId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImagePath(), item.isAvailable(), item.getMenuVariantOf());
        items.put(id, saved);
        return copy(saved);
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return Optional.ofNullable(items.get(id)).map(this::copy);
    }

    @Override
    public List<MenuItem> findByCategoryId(UUID categoryId) {
        return items.values().stream()
                .filter(item -> categoryId.equals(item.getCategoryId()))
                .map(this::copy)
                .toList();
    }

    @Override
    public List<MenuItem> findAllById(List<UUID> ids) {
        return ids.stream()
                .map(items::get)
                .filter(item -> item != null)
                .map(this::copy)
                .toList();
    }

    @Override
    public void deleteById(UUID id) {
        items.remove(id);
    }

    private MenuItem copy(MenuItem item) {
        return MenuItem.from(item.getId(), item.getCategoryId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImagePath(), item.isAvailable(), item.getMenuVariantOf());
    }
}
