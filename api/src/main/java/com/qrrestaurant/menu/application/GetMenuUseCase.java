package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.MenuView;
import com.qrrestaurant.menu.application.dto.CategoryView;

import com.qrrestaurant.menu.domain.*;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class GetMenuUseCase {

    private final RestaurantRepository restaurantRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuCompositionRepository compositionRepository;

    public GetMenuUseCase(RestaurantRepository restaurantRepository,
                          CategoryRepository categoryRepository,
                          MenuItemRepository menuItemRepository,
                          MenuCompositionRepository compositionRepository) {
        this.restaurantRepository = restaurantRepository;
        this.categoryRepository = categoryRepository;
        this.menuItemRepository = menuItemRepository;
        this.compositionRepository = compositionRepository;
    }

    public MenuView execute(String slug) {
        Restaurant restaurant = restaurantRepository.findBySlug(slug)
                .orElseThrow(() -> new RestaurantNotFoundException(slug));

        UUID restaurantId = restaurant.getId();

        List<Category> categories = categoryRepository.findByRestaurantIdOrderByPosition(restaurantId);
        Map<UUID, Category> categoriesById = categories.stream()
                .collect(Collectors.toMap(Category::getId, Function.identity()));

        List<MenuView.CategoryView> categoryViews = categories.stream()
                .map(category -> toCategoryView(category, getItemsForCategory(category.getId())))
                .toList();

        List<MenuComposition> compositions = compositionRepository.findByRestaurantId(restaurantId);
        List<MenuView.CompositionEntry> compositionEntries = toCompositionEntries(compositions, categoriesById);

        return new MenuView(toRestaurantInfo(restaurant), categoryViews, compositionEntries);
    }

    private List<MenuView.CompositionEntry> toCompositionEntries(List<MenuComposition> compositions,
                                                                 Map<UUID, Category> categoriesById) {
        List<UUID> itemIds = compositions.stream()
                .map(MenuComposition::getMenuItemId)
                .distinct()
                .toList();

        Map<UUID, MenuItem> items = menuItemRepository.findAllById(itemIds).stream()
                .filter(MenuItem::isAvailable)
                .filter(item -> !item.isMenuVariant())
                .filter(item -> categoriesById.containsKey(item.getCategoryId()))
                .collect(Collectors.toMap(MenuItem::getId, Function.identity()));

        return compositions.stream()
                .filter(composition -> items.containsKey(composition.getMenuItemId()))
                .map(c -> {
                    MenuItem item = items.get(c.getMenuItemId());
                    return new MenuView.CompositionEntry(
                            c.getId(),
                            c.getCompositionType().name(),
                            c.getMenuItemId(),
                            item.getName(),
                            item.getImagePath(),
                            c.getSupplementPrice()
                    );
                })
                .toList();
    }

    private MenuView.RestaurantInfo toRestaurantInfo(Restaurant r) {
        return new MenuView.RestaurantInfo(
                r.getId(), r.getName(), r.getSlug(),
                r.getAddress(), r.getLogoPath(), r.getThemeId()
        );
    }

    private MenuView.CategoryView toCategoryView(Category c, List<MenuView.ItemView> items) {
        return new MenuView.CategoryView(
                c.getId(), c.getName(), c.getImagePath(),
                c.getPosition(), c.isHasMenu(), items
        );
    }

    private List<MenuView.ItemView> getItemsForCategory(UUID categoryId) {
        return menuItemRepository.findByCategoryId(categoryId).stream()
                .filter(MenuItem::isAvailable)
                .map(this::toItemView)
                .toList();
    }

    private MenuView.ItemView toItemView(MenuItem item) {
        return new MenuView.ItemView(
                item.getId(), item.getName(), item.getDescription(),
                item.getPrice(), item.getImagePath(), item.isAvailable(),
                item.getMenuVariantOf()
        );
    }

    public static class RestaurantNotFoundException extends RuntimeException {
        public RestaurantNotFoundException(String slug) {
            super("Restaurant introuvable: " + slug);
        }
    }
}
