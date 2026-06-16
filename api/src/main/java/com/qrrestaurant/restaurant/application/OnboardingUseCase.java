package com.qrrestaurant.restaurant.application;
import com.qrrestaurant.restaurant.application.dto.OnboardingResponse;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class OnboardingUseCase {

    private final RestaurantTableRepository tableRepository;
    private final RestaurantSlugGenerator restaurantSlugGenerator;
    private final RestaurantRepository restaurantRepository;

    public OnboardingUseCase(RestaurantRepository restaurantRepository,
                             RestaurantTableRepository tableRepository,
                             RestaurantSlugGenerator restaurantSlugGenerator) {
        this.tableRepository = tableRepository;
        this.restaurantSlugGenerator = restaurantSlugGenerator;
        this.restaurantRepository = restaurantRepository;
    }

    public OnboardingResponse execute(UUID userId, String name, int tableCount, String themeId, String logoPath) {
        restaurantRepository.findByUserId(userId).ifPresent(existing -> {
            throw new OwnerAlreadyHasRestaurantException();
        });

        String slug = restaurantSlugGenerator.generate(name);

        Restaurant restaurant = new Restaurant();
        restaurant.setUserId(userId);
        restaurant.setName(name);
        restaurant.setSlug(slug);
        restaurant.setThemeId(RestaurantTheme.normalizeOrDefault(themeId));
        if (logoPath != null) {
            restaurant.setLogoPath(logoPath);
        }
        Restaurant saved;
        try {
            saved = restaurantRepository.save(restaurant);
        } catch (DataIntegrityViolationException ex) {
            throw new OwnerAlreadyHasRestaurantException();
        }

        List<OnboardingResponse.TableView> tables = new ArrayList<>();
        for (int i = 1; i <= tableCount; i++) {
            RestaurantTable table = new RestaurantTable();
            table.setRestaurantId(saved.getId());
            table.setNumber(i);
            RestaurantTable savedTable = tableRepository.save(table);
            tables.add(new OnboardingResponse.TableView(savedTable.getId(), i));
        }

        return new OnboardingResponse(saved.getId(), slug, name, tables, saved.getLogoPath());
    }

    public static class OwnerAlreadyHasRestaurantException extends RuntimeException {
        public OwnerAlreadyHasRestaurantException() {
            super("Cet utilisateur possède déjà un restaurant");
        }
    }
}
