package com.qrrestaurant.payment.connect.application;

import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;
import com.qrrestaurant.payment.connect.domain.StripeConnectAccountGateway;
import com.qrrestaurant.restaurant.application.GetRestaurantUseCase;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Point d'entrée du flux Stripe Connect : crée le compte Express du
 * restaurateur si nécessaire (le champ payment_provider_account_id est rempli
 * automatiquement, jamais saisi à la main), puis renvoie l'URL du formulaire
 * d'onboarding Stripe à ouvrir.
 */
@Service
@Transactional
public class StartStripeOnboardingUseCase {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final StripeConnectAccountGateway connectGateway;
    private final String adminBaseUrl;

    public StartStripeOnboardingUseCase(RestaurantRepository restaurantRepository,
                                        UserRepository userRepository,
                                        StripeConnectAccountGateway connectGateway,
                                        @Value("${app.admin-base-url}") String adminBaseUrl) {
        this.restaurantRepository = restaurantRepository;
        this.userRepository = userRepository;
        this.connectGateway = connectGateway;
        this.adminBaseUrl = adminBaseUrl;
    }

    public OnboardingView execute(UUID userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(GetRestaurantUseCase.NoRestaurantException::new);

        String accountId = restaurant.getPaymentProviderAccountId();
        if (accountId == null || accountId.isBlank()) {
            accountId = createAndAttachAccount(restaurant);
        }

        String url = connectGateway.createOnboardingLink(
                accountId,
                adminBaseUrl + "/settings?connect=return",
                adminBaseUrl + "/settings?connect=refresh");
        return new OnboardingView(url, accountId);
    }

    private String createAndAttachAccount(Restaurant restaurant) {
        String ownerEmail = userRepository.findById(restaurant.getUserId())
                .map(User::getEmail)
                .orElse(null);
        String accountId = connectGateway.createExpressAccount(ownerEmail, restaurant.getName(), restaurant.getId());
        restaurant.markStripeConnectPending(accountId);
        restaurantRepository.save(restaurant);
        return accountId;
    }

    public record OnboardingView(String url, String paymentProviderAccountId) {}
}
