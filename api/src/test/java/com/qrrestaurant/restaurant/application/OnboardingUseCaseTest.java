package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.application.dto.OnboardingResponse;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
import com.qrrestaurant.restaurant.infrastructure.persistence.table.InMemoryRestaurantTableRepository;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OnboardingUseCaseTest {

    @Test
    void persistsLogoPathProvidedDuringOnboarding() {
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        RestaurantSlugGenerator slugGenerator = new RestaurantSlugGenerator(restaurantRepository);
        OnboardingUseCase useCase = new OnboardingUseCase(
                restaurantRepository, tableRepository, slugGenerator);

        UUID userId = UUID.randomUUID();
        String logoPath = "http://localhost:8333/logos/abc-bistro-bruno.png";

        OnboardingResponse response = useCase.execute(userId, "Bistro Bruno", 4, "chaud", logoPath);

        assertThat(response.logoPath()).isEqualTo(logoPath);
        assertThat(restaurantRepository.findByUserId(userId))
                .get()
                .extracting(Restaurant::getLogoPath)
                .isEqualTo(logoPath);
    }

    @Test
    void onboardsWithoutLogoPathWhenNoneProvided() {
        InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
        InMemoryRestaurantTableRepository tableRepository = new InMemoryRestaurantTableRepository();
        RestaurantSlugGenerator slugGenerator = new RestaurantSlugGenerator(restaurantRepository);
        OnboardingUseCase useCase = new OnboardingUseCase(
                restaurantRepository, tableRepository, slugGenerator);

        UUID userId = UUID.randomUUID();

        OnboardingResponse response = useCase.execute(userId, "Bistro Serein", 2, "classique", null);

        assertThat(response.logoPath()).isNull();
        assertThat(restaurantRepository.findByUserId(userId))
                .get()
                .extracting(Restaurant::getLogoPath)
                .isNull();
    }
}
