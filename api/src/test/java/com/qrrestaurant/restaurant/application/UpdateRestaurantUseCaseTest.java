package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class UpdateRestaurantUseCaseTest {

    private static final String CLIENT_BASE_URL = "http://localhost:4300";

    @Test
    void shouldDeletePreviousLogoWhenLogoIsReplaced() {
        var ctx = setup("http://localhost:8333/logos/old.png");
        String newLogo = "http://localhost:8333/logos/new.png";

        ctx.useCase.execute(ctx.ownerId, "Bistro", null, newLogo, null, null);

        verify(ctx.storage).delete("http://localhost:8333/logos/old.png");
        verify(ctx.storage, never()).delete(newLogo);
    }

    @Test
    void shouldDeleteLogoWhenRemovedViaBlankValue() {
        var ctx = setup("http://localhost:8333/logos/old.png");

        // Le front admin envoie logoPath="" pour supprimer le logo.
        ctx.useCase.execute(ctx.ownerId, "Bistro", null, "", null, null);

        verify(ctx.storage).delete("http://localhost:8333/logos/old.png");
    }

    @Test
    void shouldNotTouchStorageWhenLogoIsUnchanged() {
        String logo = "http://localhost:8333/logos/keep.png";
        var ctx = setup(logo);

        ctx.useCase.execute(ctx.ownerId, "Bistro Renommé", null, null, null, null);

        verify(ctx.storage, never()).delete(logo);
    }

    private Context setup(String logoPath) {
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        StorageService storage = mock(StorageService.class);
        UpdateRestaurantUseCase useCase = new UpdateRestaurantUseCase(
                restaurantRepository, new ImageCleanup(storage), CLIENT_BASE_URL);

        UUID ownerId = UUID.randomUUID();
        restaurantRepository.save(Restaurant.from(
                UUID.randomUUID(), ownerId, "Bistro", "bistro", null, logoPath, "chaud", null, null));

        Context ctx = new Context();
        ctx.ownerId = ownerId;
        ctx.useCase = useCase;
        ctx.storage = storage;
        return ctx;
    }

    static class Context {
        UUID ownerId;
        UpdateRestaurantUseCase useCase;
        StorageService storage;
    }
}
