package com.qrrestaurant.restaurant.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.RestaurantTable;
import com.qrrestaurant.restaurant.domain.RestaurantTableRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@Transactional(readOnly = true)
public class GetRestaurantUseCase {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantTableRepository tableRepository;
    private final String clientBaseUrl;

    public GetRestaurantUseCase(RestaurantRepository restaurantRepository,
                                 RestaurantTableRepository tableRepository,
                                 @Value("${app.client-base-url}") String clientBaseUrl) {
        this.restaurantRepository = restaurantRepository;
        this.tableRepository = tableRepository;
        this.clientBaseUrl = clientBaseUrl;
    }

    public RestaurantView get(UUID userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);
        return toView(restaurant);
    }

    public List<TableView> getTables(UUID userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(NoRestaurantException::new);
        return tableRepository.findByRestaurantIdOrderByNumber(restaurant.getId()).stream()
                .map(t -> new TableView(t.getId(), t.getNumber()))
                .toList();
    }

    private RestaurantView toView(Restaurant r) {
        return new RestaurantView(r.getId().toString(), r.getName(), r.getSlug(),
                r.getAddress(), r.getLogoPath(), r.getThemeId(),
                r.getPaymentProviderAccountId(), clientBaseUrl);
    }

    public record RestaurantView(String id, String name, String slug, String address,
                                   String logoPath, String themeId, String paymentProviderAccountId,
                                   String clientBaseUrl) {}
    public record TableView(UUID id, int number) {}

    public static class NoRestaurantException extends RuntimeException {
        public NoRestaurantException() { super("Aucun restaurant trouvé"); }
    }
}
