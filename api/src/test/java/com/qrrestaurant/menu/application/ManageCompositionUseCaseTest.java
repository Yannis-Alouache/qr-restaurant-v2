package com.qrrestaurant.menu.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuComposition;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.composition.InMemoryMenuCompositionRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ManageCompositionUseCaseTest {

    @Test
    void shouldRejectCreatingCompositionWhenMenuItemBelongsToAnotherRestaurant() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID otherRestaurantId = UUID.randomUUID();
        UUID otherCategoryId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(new Category(otherCategoryId, otherRestaurantId, "Sides", null, 0, false));
        MenuItem foreignItem = menuItemRepository.save(new MenuItem(
                UUID.randomUUID(), otherCategoryId, "Frites", null, new BigDecimal("3.00"), null, true, null));

        ManageCompositionUseCase useCase = new ManageCompositionUseCase(
                compositionRepository, restaurantRepository, menuItemRepository, categoryRepository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.create(
                ownerId,
                "accompagnement",
                foreignItem.getId(),
                BigDecimal.ZERO));

        assertEquals("L'article de composition doit appartenir au restaurant", exception.getMessage());
    }

    @Test
    void shouldRejectDeletingCompositionOwnedByAnotherRestaurant() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID otherRestaurantId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        MenuComposition foreignComposition = compositionRepository.save(MenuComposition.from(
                UUID.randomUUID(),
                otherRestaurantId,
                MenuComposition.CompositionType.accompagnement,
                UUID.randomUUID(),
                BigDecimal.ZERO));

        ManageCompositionUseCase useCase = new ManageCompositionUseCase(
                compositionRepository, restaurantRepository, menuItemRepository, categoryRepository);

        ManageCompositionUseCase.CompositionNotFoundException exception =
                assertThrows(ManageCompositionUseCase.CompositionNotFoundException.class,
                        () -> useCase.delete(ownerId, foreignComposition.getId()));

        assertEquals("Composition introuvable", exception.getMessage());
        assertEquals(1, compositionRepository.findByRestaurantId(otherRestaurantId).size());
    }

    @Test
    void shouldRejectCreatingCompositionFromMenuVariants() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        UUID baseItemId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        InMemoryMenuCompositionRepository compositionRepository = new InMemoryMenuCompositionRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(new Category(categoryId, restaurantId, "Burgers", null, 0, true));
        MenuItem menuVariant = menuItemRepository.save(new MenuItem(
                UUID.randomUUID(), categoryId, "Menu Burger", null, new BigDecimal("10.00"), null, true, baseItemId));

        ManageCompositionUseCase useCase = new ManageCompositionUseCase(
                compositionRepository, restaurantRepository, menuItemRepository, categoryRepository);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.create(
                ownerId,
                "accompagnement",
                menuVariant.getId(),
                BigDecimal.ZERO));

        assertEquals("L'article de composition doit être un article simple disponible", exception.getMessage());
    }

    private Restaurant restaurant(UUID ownerId, UUID restaurantId) {
        Restaurant restaurant = new Restaurant();
        restaurant.setId(restaurantId);
        restaurant.setUserId(ownerId);
        restaurant.setSlug("naia-burger");
        return restaurant;
    }
}
