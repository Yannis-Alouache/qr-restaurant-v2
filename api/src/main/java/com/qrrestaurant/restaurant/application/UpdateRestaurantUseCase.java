package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.application.ImageCleanup;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final ImageCleanup imageCleanup;
    private final String clientBaseUrl;

    public UpdateRestaurantUseCase(RestaurantRepository restaurantRepository,
                                   ImageCleanup imageCleanup,
                                   @Value("${app.client-base-url}") String clientBaseUrl) {
        this.restaurantRepository = restaurantRepository;
        this.imageCleanup = imageCleanup;
        this.clientBaseUrl = clientBaseUrl;
    }

    public RestaurantView execute(UUID userId, String name, String address, String logoPath,
                                  String coverPath, String themeId, String paymentProviderAccountId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(GetRestaurantUseCase.NoRestaurantException::new);

        String previousLogo = restaurant.getLogoPath();
        String previousCover = restaurant.getCoverPath();
        restaurant.update(name, address, logoPath, coverPath, themeId, paymentProviderAccountId);

        Restaurant saved = restaurantRepository.save(restaurant);
        cleanupImageIfChanged(previousLogo, saved.getLogoPath());
        cleanupImageIfChanged(previousCover, saved.getCoverPath());
        return new RestaurantView(saved.getId().toString(), saved.getName(), saved.getSlug(),
                saved.getAddress(), saved.getLogoPath(), saved.getCoverPath(), saved.getThemeId(),
                saved.getPaymentProviderAccountId(), clientBaseUrl);
    }

    private void cleanupImageIfChanged(String previousPath, String currentPath) {
        if (previousPath != null && !previousPath.isBlank() && !previousPath.equals(currentPath)) {
            imageCleanup.delete(previousPath);
        }
    }

    public record RestaurantView(String id, String name, String slug, String address,
                                   String logoPath, String coverPath, String themeId, String paymentProviderAccountId,
                                   String clientBaseUrl) {}
}
