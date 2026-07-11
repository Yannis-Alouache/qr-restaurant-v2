package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.MenuItemView;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ManageMenuItemUseCase {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final ImageCleanup imageCleanup;

    public ManageMenuItemUseCase(MenuItemRepository menuItemRepository,
                                  CategoryRepository categoryRepository,
                                  RestaurantRepository restaurantRepository,
                                  ImageCleanup imageCleanup) {
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.imageCleanup = imageCleanup;
    }

    public MenuItemView create(UUID userId, UUID categoryId, String name, String description,
                                BigDecimal price, String imagePath, UUID menuVariantOf) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = verifyCategoryBelongsToRestaurant(categoryId, restaurant);
        validateMenuVariant(category, menuVariantOf, restaurant);

        MenuItem item = MenuItem.create(categoryId, name, description, price, imagePath, menuVariantOf);

        MenuItem saved = menuItemRepository.save(item);
        return toView(saved);
    }

    public MenuItemView update(UUID userId, UUID itemId, String name, String description,
                                BigDecimal price, String imagePath, UUID menuVariantOf) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(MenuItemNotFoundException::new);
        Category category = verifyCategoryBelongsToRestaurant(item.getCategoryId(), restaurant);
        validateMenuVariant(category, menuVariantOf, restaurant);

        String previousImage = item.getImagePath();
        item.update(name, description, price, imagePath, menuVariantOf);

        MenuItem saved = menuItemRepository.save(item);
        cleanupIfChanged(previousImage, saved.getImagePath());
        return toView(saved);
    }

    public void setAvailability(UUID userId, UUID itemId, boolean available) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(MenuItemNotFoundException::new);
        verifyCategoryBelongsToRestaurant(item.getCategoryId(), restaurant);
        item.changeAvailability(available);
        menuItemRepository.save(item);
    }

    public void delete(UUID userId, UUID itemId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        MenuItem item = menuItemRepository.findById(itemId)
                .orElseThrow(MenuItemNotFoundException::new);
        verifyCategoryBelongsToRestaurant(item.getCategoryId(), restaurant);
        menuItemRepository.deleteById(itemId);
        imageCleanup.delete(item.getImagePath());
    }

    private void cleanupIfChanged(String previousImage, String currentImage) {
        if (previousImage != null && !previousImage.isBlank() && !previousImage.equals(currentImage)) {
            imageCleanup.delete(previousImage);
        }
    }

    private Restaurant getRestaurantByOwner(UUID userId) {
        return restaurantRepository.findByUserId(userId)
                .orElseThrow(ManageCategoryUseCase.NoRestaurantException::new);
    }

    private Category verifyCategoryBelongsToRestaurant(UUID categoryId, Restaurant restaurant) {
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("Catégorie introuvable"));
        if (!category.getRestaurantId().equals(restaurant.getId())) {
            throw new MenuItemNotFoundException();
        }
        return category;
    }

    private void validateMenuVariant(Category category, UUID menuVariantOf, Restaurant restaurant) {
        if (menuVariantOf == null) {
            return;
        }
        if (!category.isHasMenu()) {
            throw new InvalidMenuVariantException("Cette catégorie ne permet pas de créer de variante menu");
        }

        MenuItem baseItem = menuItemRepository.findById(menuVariantOf)
                .orElseThrow(() -> new InvalidMenuVariantException("Le plat de base référencé est introuvable"));
        verifyCategoryBelongsToRestaurant(baseItem.getCategoryId(), restaurant);

        if (!baseItem.getCategoryId().equals(category.getId())) {
            throw new InvalidMenuVariantException("La variante menu doit référencer un plat de la même catégorie");
        }
        if (baseItem.isMenuVariant()) {
            throw new InvalidMenuVariantException("Une variante menu doit référencer un plat de base");
        }
    }

    private MenuItemView toView(MenuItem i) {
        return new MenuItemView(i.getId().toString(), i.getCategoryId().toString(),
                i.getName(), i.getDescription(), i.getPrice(), i.getImagePath(),
                i.isAvailable(), i.getMenuVariantOf() != null ? i.getMenuVariantOf().toString() : null);
    }

    public static class MenuItemNotFoundException extends RuntimeException {
        public MenuItemNotFoundException() { super("Plat introuvable"); }
    }

    public static class InvalidMenuVariantException extends IllegalArgumentException {
        public InvalidMenuVariantException(String message) {
            super(message);
        }
    }
}
