package com.qrrestaurant.menu.infrastructure.persistence.item;

import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class MenuItemRepositoryAdapter implements MenuItemRepository {

    private final MenuItemJpaRepository jpaRepo;

    public MenuItemRepositoryAdapter(MenuItemJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public List<MenuItem> findByCategoryId(UUID categoryId) {
        return jpaRepo.findByCategoryIdAndDeletedAtIsNull(categoryId).stream()
                .map(this::toDomain).toList();
    }

    @Override
    public MenuItem save(MenuItem item) {
        MenuItemJpaEntity entity = toEntity(item);
        MenuItemJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<MenuItem> findById(UUID id) {
        return jpaRepo.findByIdAndDeletedAtIsNull(id).map(this::toDomain);
    }

    @Override
    public List<MenuItem> findAllById(List<UUID> ids) {
        return jpaRepo.findAllByIdInAndDeletedAtIsNull(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public List<MenuItem> findByMenuVariantOf(UUID baseItemId) {
        return jpaRepo.findByMenuVariantOfAndDeletedAtIsNull(baseItemId).stream()
                .map(this::toDomain).toList();
    }

    private MenuItem toDomain(MenuItemJpaEntity e) {
        return MenuItem.from(e.getId(), e.getCategoryId(), e.getName(),
                e.getDescription(), e.getPrice(), e.getImagePath(),
                e.isAvailable(), e.getMenuVariantOf(), e.getDeletedAt());
    }

    private MenuItemJpaEntity toEntity(MenuItem d) {
        MenuItemJpaEntity e = new MenuItemJpaEntity();
        e.setId(d.getId());
        e.setCategoryId(d.getCategoryId());
        e.setName(d.getName());
        e.setDescription(d.getDescription());
        e.setPrice(d.getPrice());
        e.setImagePath(d.getImagePath());
        e.setAvailable(d.isAvailable());
        e.setMenuVariantOf(d.getMenuVariantOf());
        e.setDeletedAt(d.getDeletedAt());
        return e;
    }
}
