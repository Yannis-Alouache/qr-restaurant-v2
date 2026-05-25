package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class UpdateRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final String clientBaseUrl;

    public UpdateRestaurantUseCase(RestaurantRepository restaurantRepository,
                                   @Value("${app.client-base-url}") String clientBaseUrl) {
        this.restaurantRepository = restaurantRepository;
        this.clientBaseUrl = clientBaseUrl;
    }

    public RestaurantView execute(UUID userId, String name, String address, String logoPath,
                                  String themeId, String paymentProviderAccountId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(GetRestaurantUseCase.NoRestaurantException::new);

        if (name != null) restaurant.setName(name);
        if (address != null) restaurant.setAddress(address);
        if (logoPath != null) restaurant.setLogoPath(logoPath);
        if (themeId != null) restaurant.setThemeId(RestaurantTheme.normalizeOrDefault(themeId));
        if (paymentProviderAccountId != null) restaurant.setPaymentProviderAccountId(paymentProviderAccountId);

        Restaurant saved = restaurantRepository.save(restaurant);
        return new RestaurantView(saved.getId().toString(), saved.getName(), saved.getSlug(),
                saved.getAddress(), saved.getLogoPath(), saved.getThemeId(),
                saved.getPaymentProviderAccountId(), clientBaseUrl);
    }

    public record RestaurantView(String id, String name, String slug, String address,
                                   String logoPath, String themeId, String paymentProviderAccountId,
                                   String clientBaseUrl) {}
}
