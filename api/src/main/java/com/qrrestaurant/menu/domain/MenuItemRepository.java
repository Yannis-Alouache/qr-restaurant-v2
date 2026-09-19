package com.qrrestaurant.menu.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Contrat de persistance des articles : les méthodes de lecture excluent toujours
 * les articles supprimés logiquement (deleted_at non nul). La suppression passe par
 * {@link MenuItem#delete()} puis {@link #save(MenuItem)} — jamais par une suppression
 * physique, réservée aux cascades SQL.
 */
public interface MenuItemRepository {
    MenuItem save(MenuItem item);
    Optional<MenuItem> findById(UUID id);
    List<MenuItem> findByCategoryId(UUID categoryId);
    List<MenuItem> findAllById(List<UUID> ids);
    List<MenuItem> findByMenuVariantOf(UUID baseItemId);
}
