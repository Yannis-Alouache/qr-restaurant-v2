package com.qrrestaurant.payment.connect.application;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.restaurant.domain.StripeConnectStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Traite les webhooks Stripe account.updated : le statut d'onboarding du
 * restaurant est mis à jour en continu sans attendre que le restaurateur
 * revienne sur l'application (source de vérité = Stripe).
 */
@Service
@Transactional
public class HandleStripeAccountUpdatedUseCase {

    private static final Logger log = LoggerFactory.getLogger(HandleStripeAccountUpdatedUseCase.class);

    private final RestaurantRepository restaurantRepository;

    public HandleStripeAccountUpdatedUseCase(RestaurantRepository restaurantRepository) {
        this.restaurantRepository = restaurantRepository;
    }

    public void execute(String accountId, Boolean detailsSubmitted, Boolean payoutsEnabled, boolean deleted) {
        if (accountId == null || accountId.isBlank()) {
            log.warn("Webhook account.updated sans identifiant de compte, ignoré");
            return;
        }

        restaurantRepository.findByPaymentProviderAccountId(accountId).ifPresentOrElse(restaurant -> {
            if (deleted) {
                log.info("Compte Stripe {} supprimé, déconnexion du restaurant {}", accountId, restaurant.getId());
                restaurant.clearStripeConnection();
            } else {
                StripeConnectStatus status = StripeConnectStatus.fromStripeAccountState(detailsSubmitted, payoutsEnabled);
                log.info("Compte Stripe {} du restaurant {} : statut {}", accountId, restaurant.getId(), status.value());
                restaurant.updateStripeConnectStatus(status);
            }
            restaurantRepository.save(restaurant);
        }, () ->
                // Événement pour un compte non rattaché (créé hors application,
                // déjà nettoyé) : acquitté sans traitement.
                log.info("Webhook account.updated pour un compte Stripe inconnu ({}), ignoré", accountId));
    }
}
