package com.qrrestaurant.menu.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuCompositionRepository;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetAdminMenuUseCase {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCompositionRepository compositionRepository;

    public GetAdminMenuUseCase(RestaurantRepository restaurantRepository,
                               CategoryRepository categoryRepository,
                               MenuItemRepository menuItemRepository,
                               MenuCompositionRepository compositionRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.compositionRepository = compositionRepository;
    }

    public List<CategoryView> getCategories(UUID userId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        return categoryRepository.findByRestaurantIdOrderByPosition(restaurant.getId()).stream()
                .map(this::toCategoryView)
                .toList();
    }

    public List<MenuItemView> getMenuItems(UUID userId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        return categoryRepository.findByRestaurantIdOrderByPosition(restaurant.getId()).stream()
                .flatMap(category -> menuItemRepository.findByCategoryId(category.getId()).stream())
                .map(this::toMenuItemView)
                .toList();
    }

    public List<CompositionView> getCompositions(UUID userId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        return compositionRepository.findByRestaurantId(restaurant.getId()).stream()
                .map(this::toCompositionView)
                .toList();
    }

    private Restaurant getRestaurantByOwner(UUID userId) {
        return restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);
    }

    private CategoryView toCategoryView(Category category) {
        return new CategoryView(
                category.getId().toString(),
                category.getName(),
                category.getImagePath(),
                category.getPosition(),
                category.isHasMenu()
        );
    }

    private MenuItemView toMenuItemView(MenuItem menuItem) {
        return new MenuItemView(
                menuItem.getId().toString(),
                menuItem.getCategoryId().toString(),
                menuItem.getName(),
                menuItem.getDescription(),
                menuItem.getPrice(),
                menuItem.getImagePath(),
                menuItem.isAvailable(),
                menuItem.getMenuVariantOf() != null ? menuItem.getMenuVariantOf().toString() : null
        );
    }

    private CompositionView toCompositionView(MenuComposition composition) {
        return new CompositionView(
                composition.getId().toString(),
                composition.getCompositionType().name(),
                composition.getMenuItemId().toString(),
                composition.getSupplementPrice()
        );
    }

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() {
            super("Aucun restaurant trouvé");
        }
    }
}
