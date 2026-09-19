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
        UUID id = item.getId() != null ? item.getId() : UUID.randomUUID();
        MenuItem saved = copy(item, id);
        items.put(id, saved);
        return copy(saved, id);
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return items.values().stream()
                .filter(item -> id.equals(item.getId()) && !item.isDeleted())
                .map(item -> copy(item, id))
                .findFirst();
    }

    @Override
    public List<MenuItem> findByCategoryId(UUID categoryId) {
        return items.values().stream()
                .filter(item -> categoryId.equals(item.getCategoryId()) && !item.isDeleted())
                .map(item -> copy(item, item.getId()))
                .toList();
    }

    @Override
    public List<MenuItem> findAllById(List<UUID> ids) {
        return ids.stream()
                .map(items::get)
                .filter(item -> item != null && !item.isDeleted())
                .map(item -> copy(item, item.getId()))
                .toList();
    }

    @Override
    public List<MenuItem> findByMenuVariantOf(UUID baseItemId) {
        return items.values().stream()
                .filter(item -> baseItemId.equals(item.getMenuVariantOf()) && !item.isDeleted())
                .map(item -> copy(item, item.getId()))
                .toList();
    }

    private MenuItem copy(MenuItem item, UUID id) {
        return MenuItem.from(id, item.getCategoryId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImagePath(), item.isAvailable(), item.getMenuVariantOf(),
                item.getDeletedAt());
    }
}
