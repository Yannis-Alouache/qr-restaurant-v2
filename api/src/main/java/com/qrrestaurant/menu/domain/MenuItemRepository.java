package com.qrrestaurant.menu.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MenuItemRepository {
    MenuItem save(MenuItem item);
    Optional<MenuItem> findById(UUID id);
    List<MenuItem> findByCategoryId(UUID categoryId);
    List<MenuItem> findAllById(List<UUID> ids);
    void deleteById(UUID id);
}
