package com.qrrestaurant.acceptance;

import com.fasterxml.jackson.databind.JsonNode;
import com.qrrestaurant.payment.connect.infrastructure.DeterministicConnectAccountGateway;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Parcours Stripe Connect du restaurateur : démarrage de l'onboarding depuis
 * les paramètres (le champ paiement se remplit automatiquement), montée du
 * statut via le webhook account.updated, déconnexion si le compte Stripe
 * disparaît. Le port Stripe est la doublure déterministe.
 */
class StripeConnectSmokeTest extends AcceptanceTestBase {

    @Test
    void shouldStartOnboardingAndAutomaticallyFillThePaymentAccountField() throws Exception {
        restoreSeedDemoState();
        connectGateway.reset();

        // État initial : le restaurant de seed possède déjà un acct_ (backfill) ;
        // on simule un nouveau restaurant sans aucun compte configuré.
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = NULL, stripe_connect_status = NULL WHERE id = ?",
                RESTAURANT_ID);
        Cookie owner = seedOwnerJwtCookie();

        JsonNode onboarding = postAuthorizedOkJson("/api/admin/connect/stripe/onboarding", owner);

        assertEquals(
                DeterministicConnectAccountGateway.ONBOARDING_URL_PREFIX + DeterministicConnectAccountGateway.ACCOUNT_ID,
                onboarding.path("url").asText());

        JsonNode restaurant = getAuthorizedJson("/api/admin/restaurant", owner);
        assertEquals(DeterministicConnectAccountGateway.ACCOUNT_ID,
                restaurant.path("paymentProviderAccountId").asText());
        assertEquals("pending", restaurant.path("stripeConnectStatus").asText());
    }

    @Test
    void shouldReportActiveStatusOnceStripeConfirmsOnboarding() throws Exception {
        restoreSeedDemoState();
        connectGateway.reset();
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = NULL, stripe_connect_status = NULL WHERE id = ?",
                RESTAURANT_ID);
        Cookie owner = seedOwnerJwtCookie();

        postAuthorizedOkJson("/api/admin/connect/stripe/onboarding", owner);
        connectGateway.setAccountState(DeterministicConnectAccountGateway.ACCOUNT_ID, true, true);

        JsonNode status = getAuthorizedJson("/api/admin/connect/stripe/status", owner);
        assertEquals("active", status.path("stripeConnectStatus").asText());
        assertEquals(DeterministicConnectAccountGateway.ACCOUNT_ID,
                status.path("paymentProviderAccountId").asText());

        JsonNode restaurant = getAuthorizedJson("/api/admin/restaurant", owner);
        assertEquals("active", restaurant.path("stripeConnectStatus").asText());
    }

    @Test
    void shouldMoveToActiveWhenAccountUpdatedWebhookArrives() throws Exception {
        restoreSeedDemoState();
        connectGateway.reset();
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = NULL, stripe_connect_status = NULL WHERE id = ?",
                RESTAURANT_ID);
        Cookie owner = seedOwnerJwtCookie();

        postAuthorizedOkJson("/api/admin/connect/stripe/onboarding", owner);
        assertEquals("pending", getAuthorizedJson("/api/admin/restaurant", owner)
                .path("stripeConnectStatus").asText());

        // Le restaurateur termine son inscription sur Stripe : l'événement met
        // le statut à jour sans qu'il revienne sur l'application.
        postStripeWebhook(accountUpdatedPayload(DeterministicConnectAccountGateway.ACCOUNT_ID, true, true, false));

        JsonNode restaurant = getAuthorizedJson("/api/admin/restaurant", owner);
        assertEquals("active", restaurant.path("stripeConnectStatus").asText());
    }

    @Test
    void shouldClearConnectionWhenStripeReportsTheAccountDeleted() throws Exception {
        restoreSeedDemoState();
        connectGateway.reset();
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = NULL, stripe_connect_status = NULL WHERE id = ?",
                RESTAURANT_ID);
        Cookie owner = seedOwnerJwtCookie();

        postAuthorizedOkJson("/api/admin/connect/stripe/onboarding", owner);

        postStripeWebhook(accountUpdatedPayload(DeterministicConnectAccountGateway.ACCOUNT_ID, true, true, true));

        JsonNode restaurant = getAuthorizedJson("/api/admin/restaurant", owner);
        assertTrue(restaurant.path("paymentProviderAccountId").isNull());
        assertTrue(restaurant.path("stripeConnectStatus").isNull());
    }

    @Test
    void shouldKeepPendingWhenOnboardingIsOnlyPartiallyCompleted() throws Exception {
        restoreSeedDemoState();
        connectGateway.reset();
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = NULL, stripe_connect_status = NULL WHERE id = ?",
                RESTAURANT_ID);
        Cookie owner = seedOwnerJwtCookie();

        postAuthorizedOkJson("/api/admin/connect/stripe/onboarding", owner);
        // L'inscription a commencé mais le KYC n'est pas terminé.
        connectGateway.setAccountState(DeterministicConnectAccountGateway.ACCOUNT_ID, false, false);

        JsonNode status = getAuthorizedJson("/api/admin/connect/stripe/status", owner);
        assertEquals("pending", status.path("stripeConnectStatus").asText());
    }

    @Test
    void shouldRejectOnboardingForAnonymousCaller() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/admin/connect/stripe/onboarding"))
                .andExpect(MockMvcResultMatchers.status().isUnauthorized());
    }
}
