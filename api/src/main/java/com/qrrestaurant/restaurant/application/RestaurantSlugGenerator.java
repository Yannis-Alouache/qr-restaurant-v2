package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.stereotype.Service;

import java.text.Normalizer;

@Service
public class RestaurantSlugGenerator {

    static final int MAX_SLUG_RETRIES = 50;

    private final RestaurantRepository restaurantRepository;

    public RestaurantSlugGenerator(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public String generate(String name) {
        String baseSlug = slugify(name);
        if (baseSlug.isBlank()) {
            throw new SlugGenerationException();
        }

        String slug = baseSlug;
        for (int i = 1; i <= MAX_SLUG_RETRIES; i++) {
            if (!restaurantRepository.existsBySlug(slug)) {
                return slug;
            }
            slug = baseSlug + "-" + i;
        }

        throw new SlugGenerationException();
    }

    static String slugify(String name) {
        return Normalizer.normalize(name, Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-")
                .replaceAll("^-|-$", "");
    }

    public static class SlugGenerationException extends RuntimeException {
        public SlugGenerationException() {
            super("Impossible de générer un identifiant unique");
        }
    }
}
