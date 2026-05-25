package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.InMemoryRestaurantRepository;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestaurantSlugGeneratorTest {

    private final InMemoryRestaurantRepository restaurantRepository = new InMemoryRestaurantRepository();
    private final RestaurantSlugGenerator generator = new RestaurantSlugGenerator(restaurantRepository);

    @Test
    void shouldSlugifyNamesAndRemoveAccents() {
        String slug = generator.generate("L'Appétit Moderne");

        assertEquals("lappetit-moderne", slug);
    }

    @Test
    void shouldAppendNumericSuffixWhenSlugAlreadyExists() {
        Restaurant existingRestaurant = new Restaurant();
        existingRestaurant.setSlug("naia-burger");
        restaurantRepository.save(existingRestaurant);

        String slug = generator.generate("Naia Burger");

        assertEquals("naia-burger-1", slug);
    }

    @Test
    void shouldFailAfterFiftyAttempts() {
        for (int index = 0; index <= RestaurantSlugGenerator.MAX_SLUG_RETRIES; index++) {
            Restaurant existingRestaurant = new Restaurant();
            existingRestaurant.setSlug(index == 0 ? "naia-burger" : "naia-burger-" + index);
            restaurantRepository.save(existingRestaurant);
        }

        assertThrows(RestaurantSlugGenerator.SlugGenerationException.class,
                () -> generator.generate("Naia Burger"));
    }
}
