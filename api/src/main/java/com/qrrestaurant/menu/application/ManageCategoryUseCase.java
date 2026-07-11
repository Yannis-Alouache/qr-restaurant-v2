package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.CategoryView;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ManageCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;
    private final ImageCleanup imageCleanup;

    public ManageCategoryUseCase(CategoryRepository categoryRepository,
                                  RestaurantRepository restaurantRepository,
                                  ImageCleanup imageCleanup) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
        this.imageCleanup = imageCleanup;
    }

    public CategoryView create(UUID userId, String name, String imagePath, Integer position, boolean hasMenu) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = Category.create(restaurant.getId(), name, imagePath, position, hasMenu);
        Category saved = categoryRepository.save(category);
        return toView(saved);
    }

    public CategoryView update(UUID userId, UUID categoryId, String name, String imagePath, Integer position, Boolean hasMenu) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        verifyOwnership(category, restaurant);
        String previousImage = category.getImagePath();
        category.update(name, imagePath, position, hasMenu);
        Category saved = categoryRepository.save(category);
        cleanupIfChanged(previousImage, saved.getImagePath());
        return toView(saved);
    }

    public void delete(UUID userId, UUID categoryId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        verifyOwnership(category, restaurant);
        categoryRepository.deleteById(categoryId);
        imageCleanup.delete(category.getImagePath());
    }

    private void cleanupIfChanged(String previousImage, String currentImage) {
        if (previousImage != null && !previousImage.isBlank() && !previousImage.equals(currentImage)) {
            imageCleanup.delete(previousImage);
        }
    }

    private Restaurant getRestaurantByOwner(UUID userId) {
        return restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);
    }

    private void verifyOwnership(Category category, Restaurant restaurant) {
        if (!category.getRestaurantId().equals(restaurant.getId())) {
            throw new CategoryNotFoundException();
        }
    }

    private CategoryView toView(Category c) {
        return new CategoryView(c.getId().toString(), c.getName(), c.getImagePath(),
                c.getPosition(), c.isHasMenu());
    }

    public static class CategoryNotFoundException extends RuntimeException {
        public CategoryNotFoundException() { super("Catégorie introuvable"); }
    }

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant trouvé pour cet utilisateur"); }
    }
}
