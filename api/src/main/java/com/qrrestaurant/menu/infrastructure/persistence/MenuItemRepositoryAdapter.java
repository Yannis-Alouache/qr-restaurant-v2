package com.qrrestaurant.menu.infrastructure.persistence;

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
        return jpaRepo.findByCategoryId(categoryId).stream()
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
        return jpaRepo.findById(id).map(this::toDomain);
    }

    @Override
    public List<MenuItem> findAllById(List<UUID> ids) {
        return jpaRepo.findAllById(ids).stream().map(this::toDomain).toList();
    }

    @Override
    public void deleteById(UUID id) {
        jpaRepo.deleteById(id);
    }

    private MenuItem toDomain(MenuItemJpaEntity e) {
        return new MenuItem(e.getId(), e.getCategoryId(), e.getName(),
                e.getDescription(), e.getPrice(), e.getImagePath(),
                e.isAvailable(), e.getMenuVariantOf());
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
        return e;
    }
}
