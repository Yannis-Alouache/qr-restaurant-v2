package com.qrrestaurant.payment.connect.application;

import com.qrrestaurant.payment.connect.domain.StripeConnectAccountGateway;
import com.qrrestaurant.restaurant.application.GetRestaurantUseCase;
import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.StripeConnectStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Relit l'état du compte Stripe Connect auprès de Stripe et le persiste :
 * appelé au retour du formulaire d'onboarding (et rafraîchissable à la main),
 * le webhook account.updated tenant le statut à jour le reste du temps.
 */
@Service
@Transactional
public class RefreshStripeConnectStatusUseCase {

    private final RestaurantRepository restaurantRepository;
    private final StripeConnectAccountGateway connectGateway;

    public RefreshStripeConnectStatusUseCase(RestaurantRepository restaurantRepository,
                                             StripeConnectAccountGateway connectGateway) {
        this.restaurantRepository = restaurantRepository;
        this.connectGateway = connectGateway;
    }

    public StripeConnectStatusView execute(UUID userId) {
        Restaurant restaurant = restaurantRepository.findByUserId(userId)
                .orElseThrow(GetRestaurantUseCase.NoRestaurantException::new);

        String accountId = restaurant.getPaymentProviderAccountId();
        if (accountId == null || accountId.isBlank()) {
            return new StripeConnectStatusView(null, null);
        }

        StripeConnectAccountGateway.StripeConnectAccountState state = connectGateway.retrieveAccount(accountId);
        if (state.deleted()) {
            // Le compte n'existe plus chez Stripe : on déconnecte proprement,
            // le restaurateur repart d'un nouvel onboarding.
            restaurant.clearStripeConnection();
            restaurantRepository.save(restaurant);
            return new StripeConnectStatusView(null, null);
        }

        StripeConnectStatus status = StripeConnectStatus.fromStripeAccountState(
                state.detailsSubmitted(), state.payoutsEnabled());
        restaurant.updateStripeConnectStatus(status);
        restaurantRepository.save(restaurant);
        return new StripeConnectStatusView(accountId, status.value());
    }

    public record StripeConnectStatusView(String paymentProviderAccountId, String stripeConnectStatus) {}
}
