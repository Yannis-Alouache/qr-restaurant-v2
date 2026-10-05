package com.qrrestaurant.payment.connect.infrastructure;

import com.qrrestaurant.payment.connect.domain.StripeConnectAccountGateway;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Doublure déterministe de l'API Stripe Connect pour les tests d'acceptance :
 * aucun appel réseau, états de compte pilotés par les tests via
 * {@link #setAccountState}. Même philosophique que DeterministicPaymentGateway.
 */
public class DeterministicConnectAccountGateway implements StripeConnectAccountGateway {

    public static final String ACCOUNT_ID = "acct_deterministic_test";
    public static final String ONBOARDING_URL_PREFIX = "https://connect.stripe.test/onboarding/";

    private final Map<String, StripeConnectAccountState> accounts = new ConcurrentHashMap<>();

    @Override
    public String createExpressAccount(String ownerEmail, String restaurantName, UUID restaurantId) {
        accounts.putIfAbsent(ACCOUNT_ID, new StripeConnectAccountState(false, false, false));
        return ACCOUNT_ID;
    }

    @Override
    public String createOnboardingLink(String accountId, String returnUrl, String refreshUrl) {
        return ONBOARDING_URL_PREFIX + accountId;
    }

    @Override
    public StripeConnectAccountState retrieveAccount(String accountId) {
        return accounts.getOrDefault(accountId, new StripeConnectAccountState(null, null, true));
    }

    /** Simule l'évolution du compte Stripe (ex. après un webhook account.updated). */
    public void setAccountState(String accountId, Boolean detailsSubmitted, Boolean payoutsEnabled) {
        accounts.put(accountId, new StripeConnectAccountState(detailsSubmitted, payoutsEnabled, false));
    }

    public void reset() {
        accounts.clear();
    }
}
