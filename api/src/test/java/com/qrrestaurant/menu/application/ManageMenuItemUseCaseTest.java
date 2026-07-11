package com.qrrestaurant.menu.application;
import com.qrrestaurant.menu.application.dto.MenuItemView;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.domain.MenuItem;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.menu.infrastructure.persistence.item.InMemoryMenuItemRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ManageMenuItemUseCaseTest {

    @Test
    void shouldRejectCreatingMenuVariantWhenCategoryDoesNotAllowMenus() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, false));
        MenuItem baseItem = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), categoryId, "Burger", null, new BigDecimal("12.00"), null, true, null));

        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository,
                new ImageCleanup(mock(StorageService.class)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.create(
                ownerId,
                categoryId,
                "Menu Burger",
                null,
                new BigDecimal("15.00"),
                null,
                baseItem.getId()));

        assertEquals("Cette catégorie ne permet pas de créer de variante menu", exception.getMessage());
    }

    @Test
    void shouldRejectCreatingMenuVariantWhenBaseItemBelongsToAnotherCategory() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID burgersCategoryId = UUID.randomUUID();
        UUID dessertsCategoryId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(burgersCategoryId, restaurantId, "Burgers", null, 0, true));
        categoryRepository.save(Category.from(dessertsCategoryId, restaurantId, "Desserts", null, 1, true));
        MenuItem baseItem = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), dessertsCategoryId, "Brownie", null, new BigDecimal("5.00"), null, true, null));

        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository,
                new ImageCleanup(mock(StorageService.class)));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> useCase.create(
                ownerId,
                burgersCategoryId,
                "Menu Brownie",
                null,
                new BigDecimal("8.00"),
                null,
                baseItem.getId()));

        assertEquals("La variante menu doit référencer un plat de la même catégorie", exception.getMessage());
    }

    @Test
    void shouldClearDescriptionWhenUpdatingWithBlankValue() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();

        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));
        MenuItem item = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), categoryId, "Burger", "Description initiale",
                new BigDecimal("12.00"), null, true, null));

        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository,
                new ImageCleanup(mock(StorageService.class)));

        MenuItemView updated = useCase.update(
                ownerId,
                item.getId(),
                null,
                "",
                null,
                null,
                null);

        assertEquals(null, updated.description());
    }

    private Restaurant restaurant(UUID ownerId, UUID restaurantId) {
        return Restaurant.from(restaurantId, ownerId, null, "naia-burger", null, null, "classique", null, null);
    }

    @Test
    void shouldDeleteStoredImageWhenDeletingItem() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String oldImage = "http://localhost:8333/menu-images/old.png";

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));
        MenuItem item = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), categoryId, "Burger", null, new BigDecimal("12.00"), oldImage, true, null));

        StorageService storage = mock(StorageService.class);
        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository, new ImageCleanup(storage));

        useCase.delete(ownerId, item.getId());

        verify(storage).delete(oldImage);
    }

    @Test
    void shouldDeletePreviousImageWhenItemImageIsReplaced() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String oldImage = "http://localhost:8333/menu-images/old.png";
        String newImage = "http://localhost:8333/menu-images/new.png";

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));
        MenuItem item = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), categoryId, "Burger", null, new BigDecimal("12.00"), oldImage, true, null));

        StorageService storage = mock(StorageService.class);
        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository, new ImageCleanup(storage));

        useCase.update(ownerId, item.getId(), "Burger", null, new BigDecimal("12.00"), newImage, null);

        verify(storage).delete(oldImage);
        verify(storage, never()).delete(newImage);
    }

    @Test
    void shouldNotTouchStorageWhenItemUpdatedWithoutChangingImage() {
        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        UUID categoryId = UUID.randomUUID();
        String image = "http://localhost:8333/menu-images/keep.png";

        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        InMemoryMenuItemRepository menuItemRepository = new InMemoryMenuItemRepository();
        restaurantRepository.save(restaurant(ownerId, restaurantId));
        categoryRepository.save(Category.from(categoryId, restaurantId, "Burgers", null, 0, true));
        MenuItem item = menuItemRepository.save(MenuItem.from(
                UUID.randomUUID(), categoryId, "Burger", null, new BigDecimal("12.00"), image, true, null));

        StorageService storage = mock(StorageService.class);
        ManageMenuItemUseCase useCase = new ManageMenuItemUseCase(
                menuItemRepository, categoryRepository, restaurantRepository, new ImageCleanup(storage));

        // imagePath null => inchangé, seul le prix change.
        useCase.update(ownerId, item.getId(), "Burger", null, new BigDecimal("14.00"), null, null);

        verify(storage, never()).delete(image);
    }
}
