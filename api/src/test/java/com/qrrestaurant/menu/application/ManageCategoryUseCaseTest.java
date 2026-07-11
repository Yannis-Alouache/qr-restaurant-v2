package com.qrrestaurant.menu.application;

import com.qrrestaurant.menu.domain.Category;
import com.qrrestaurant.menu.infrastructure.persistence.category.InMemoryCategoryRepository;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ManageCategoryUseCaseTest {

    @Test
    void shouldDeleteStoredImageWhenDeletingCategory() {
        var ctx = setup();
        Category category = ctx.categoryRepository.save(Category.from(
                UUID.randomUUID(), ctx.restaurantId, "Burgers", ctx.oldImage, 0, true));

        ctx.useCase.delete(ctx.ownerId, category.getId());

        verify(ctx.storage).delete(ctx.oldImage);
    }

    @Test
    void shouldDeletePreviousImageWhenCategoryImageIsReplaced() {
        var ctx = setup();
        Category category = ctx.categoryRepository.save(Category.from(
                UUID.randomUUID(), ctx.restaurantId, "Burgers", ctx.oldImage, 0, true));
        String newImage = "http://localhost:8333/category-images/new.png";

        ctx.useCase.update(ctx.ownerId, category.getId(), "Burgers", newImage, 0, true);

        verify(ctx.storage).delete(ctx.oldImage);
        verify(ctx.storage, never()).delete(newImage);
    }

    @Test
    void shouldDeletePreviousImageWhenCategoryImageIsCleared() {
        var ctx = setup();
        Category category = ctx.categoryRepository.save(Category.from(
                UUID.randomUUID(), ctx.restaurantId, "Burgers", ctx.oldImage, 0, true));

        // Front envoie "" pour retirer l'image.
        ctx.useCase.update(ctx.ownerId, category.getId(), "Burgers", "", 0, true);

        verify(ctx.storage).delete(ctx.oldImage);
    }

    @Test
    void shouldNotTouchStorageWhenCategoryUpdatedWithoutChangingImage() {
        var ctx = setup();
        Category category = ctx.categoryRepository.save(Category.from(
                UUID.randomUUID(), ctx.restaurantId, "Burgers", ctx.oldImage, 0, true));

        ctx.useCase.update(ctx.ownerId, category.getId(), "Burgers Renommés", null, 1, true);

        verify(ctx.storage, never()).delete(ctx.oldImage);
    }

    private static Context setup() {
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryCategoryRepository categoryRepository = new InMemoryCategoryRepository();
        StorageService storage = mock(StorageService.class);
        ManageCategoryUseCase useCase = new ManageCategoryUseCase(
                categoryRepository, restaurantRepository, new ImageCleanup(storage));

        UUID ownerId = UUID.randomUUID();
        UUID restaurantId = UUID.randomUUID();
        restaurantRepository.save(Restaurant.from(
                restaurantId, ownerId, null, "naia-burger", null, null, "classique", null, null));

        Context ctx = new Context();
        ctx.ownerId = ownerId;
        ctx.restaurantId = restaurantId;
        ctx.oldImage = "http://localhost:8333/category-images/old.png";
        ctx.categoryRepository = categoryRepository;
        ctx.useCase = useCase;
        ctx.storage = storage;
        return ctx;
    }

    static class Context {
        UUID ownerId;
        UUID restaurantId;
        String oldImage;
        InMemoryCategoryRepository categoryRepository;
        ManageCategoryUseCase useCase;
        StorageService storage;
    }
}
