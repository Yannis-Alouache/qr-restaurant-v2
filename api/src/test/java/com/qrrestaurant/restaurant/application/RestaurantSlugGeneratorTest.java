package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.infrastructure.persistence.restaurant.InMemoryRestaurantRepository;
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
        restaurantRepository.save(restaurantWithSlug("naia-burger"));

        String slug = generator.generate("Naia Burger");

        assertEquals("naia-burger-1", slug);
    }

    @Test
    void shouldFailAfterFiftyAttempts() {
        for (int index = 0; index <= RestaurantSlugGenerator.MAX_SLUG_RETRIES; index++) {
            restaurantRepository.save(restaurantWithSlug(index == 0 ? "naia-burger" : "naia-burger-" + index));
        }

        assertThrows(RestaurantSlugGenerator.SlugGenerationException.class,
                () -> generator.generate("Naia Burger"));
    }

    private Restaurant restaurantWithSlug(String slug) {
        return Restaurant.from(null, null, null, slug, null, null, "classique", null, null);
    }
}
