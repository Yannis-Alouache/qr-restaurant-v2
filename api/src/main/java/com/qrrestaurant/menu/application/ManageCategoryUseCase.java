package com.qrrestaurant.menu.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class ManageCategoryUseCase {

    private final CategoryRepository categoryRepository;
    private final RestaurantRepository restaurantRepository;

    public ManageCategoryUseCase(CategoryRepository categoryRepository,
                                  RestaurantRepository restaurantRepository) {
        this.categoryRepository = categoryRepository;
        this.restaurantRepository = restaurantRepository;
    }

    public CategoryView create(UUID userId, String name, String imagePath, Integer position, boolean hasMenu) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = new Category();
        category.setRestaurantId(restaurant.getId());
        category.setName(name);
        category.setImagePath(imagePath);
        category.setPosition(position != null ? position : 0);
        category.setHasMenu(hasMenu);
        Category saved = categoryRepository.save(category);
        return toView(saved);
    }

    public CategoryView update(UUID userId, UUID categoryId, String name, String imagePath, Integer position, Boolean hasMenu) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        verifyOwnership(category, restaurant);
        if (name != null) category.setName(name);
        if (imagePath != null) category.setImagePath(imagePath);
        if (position != null) category.setPosition(position);
        if (hasMenu != null) category.setHasMenu(hasMenu);
        Category saved = categoryRepository.save(category);
        return toView(saved);
    }

    public void delete(UUID userId, UUID categoryId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        Category category = categoryRepository.findById(categoryId)
                .orElseThrow(CategoryNotFoundException::new);
        verifyOwnership(category, restaurant);
        categoryRepository.deleteById(categoryId);
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
