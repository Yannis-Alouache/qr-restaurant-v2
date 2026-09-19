package com.qrrestaurant.menu.infrastructure.persistence.item;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

interface MenuItemJpaRepository extends JpaRepository<MenuItemJpaEntity, UUID> {
    Optional<MenuItemJpaEntity> findByIdAndDeletedAtIsNull(UUID id);

    List<MenuItemJpaEntity> findByCategoryIdAndDeletedAtIsNull(UUID categoryId);

    List<MenuItemJpaEntity> findAllByIdInAndDeletedAtIsNull(List<UUID> ids);

    List<MenuItemJpaEntity> findByMenuVariantOfAndDeletedAtIsNull(UUID baseItemId);
}
