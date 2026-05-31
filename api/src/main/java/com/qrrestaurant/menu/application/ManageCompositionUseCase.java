package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.CompositionView;

import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuCompositionRepository;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.domain.MenuItemRepository;
import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.CategoryRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@Transactional
public class ManageCompositionUseCase {

    private final MenuCompositionRepository compositionRepository;
    private final RestaurantRepository restaurantRepository;
    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;

    public ManageCompositionUseCase(MenuCompositionRepository compositionRepository,
                                    RestaurantRepository restaurantRepository,
                                    MenuItemRepository menuItemRepository,
                                    CategoryRepository categoryRepository) {
        this.compositionRepository = compositionRepository;
        this.restaurantRepository = restaurantRepository;
        this.menuItemRepository = menuItemRepository;
        this.categoryRepository = categoryRepository;
    }

    public CompositionView create(UUID userId, String compositionType, UUID menuItemId, BigDecimal supplementPrice) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        validateMenuItemOwnership(menuItemId, restaurant);
        MenuComposition composition = new MenuComposition();
        composition.setRestaurantId(restaurant.getId());
        composition.setCompositionType(MenuComposition.CompositionType.valueOf(compositionType));
        composition.setMenuItemId(menuItemId);
        composition.setSupplementPrice(supplementPrice != null ? supplementPrice : BigDecimal.ZERO);
        MenuComposition saved = compositionRepository.save(composition);
        return toView(saved);
    }

    public void delete(UUID userId, UUID compositionId) {
        Restaurant restaurant = getRestaurantByOwner(userId);
        boolean ownedCompositionExists = compositionRepository.findByRestaurantId(restaurant.getId()).stream()
                .anyMatch(composition -> composition.getId().equals(compositionId));
        if (!ownedCompositionExists) {
            throw new CompositionNotFoundException();
        }
        compositionRepository.deleteById(compositionId);
    }

    private Restaurant getRestaurantByOwner(UUID userId) {
        return restaurantRepository.findByUserId(userId)
                .orElseThrow(ManageCategoryUseCase.NoRestaurantException::new);
    }

    private void validateMenuItemOwnership(UUID menuItemId, Restaurant restaurant) {
        MenuItem menuItem = menuItemRepository.findById(menuItemId)
                .orElseThrow(() -> new InvalidCompositionMenuItemException("L'article de composition doit appartenir au restaurant"));
        Category category = categoryRepository.findById(menuItem.getCategoryId())
                .orElseThrow(() -> new InvalidCompositionMenuItemException("L'article de composition doit appartenir au restaurant"));
        if (!restaurant.getId().equals(category.getRestaurantId())) {
            throw new InvalidCompositionMenuItemException("L'article de composition doit appartenir au restaurant");
        }
        if (menuItem.isMenuVariant() || !menuItem.isAvailable()) {
            throw new InvalidCompositionMenuItemException("L'article de composition doit être un article simple disponible");
        }
    }

    private CompositionView toView(MenuComposition c) {
        return new CompositionView(c.getId().toString(), c.getCompositionType().name(),
                c.getMenuItemId().toString(), c.getSupplementPrice());
    }

    public static class CompositionNotFoundException extends RuntimeException {
        public CompositionNotFoundException() { super("Composition introuvable"); }
    }

    public static class InvalidCompositionMenuItemException extends IllegalArgumentException {
        public InvalidCompositionMenuItemException(String message) {
            super(message);
        }
    }
}
