package com.qrrestaurant.payment.connect.infrastructure;

import com.qrrestaurant.payment.connect.domain.StripeConnectAccountGateway;
import com.stripe.exception.InvalidRequestException;
import com.stripe.exception.StripeException;
import com.stripe.model.Account;
import com.stripe.param.AccountCreateParams;
import com.stripe.param.AccountLinkCreateParams;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class StripeSdkConnectAccountGateway implements StripeConnectAccountGateway {

    private final com.stripe.StripeClient stripeClient;
    private final String accountCountry;

    public StripeSdkConnectAccountGateway(@Value("${stripe.secret-key}") String secretKey,
                                          @Value("${stripe.connect.country:FR}") String accountCountry) {
        this.stripeClient = new com.stripe.StripeClient(secretKey);
        this.accountCountry = accountCountry;
    }

    @Override
    public String createExpressAccount(String ownerEmail, String restaurantName, UUID restaurantId) {
        AccountCreateParams.Builder params = AccountCreateParams.builder()
                // Express : le restaurateur possède son compte et ses versements,
                // la plateforme reste le marchand d'enregistrement (destination
                // charges déjà utilisées par le checkout).
                .setType(AccountCreateParams.Type.EXPRESS)
                .setCountry(accountCountry)
                .putMetadata("restaurant_id", restaurantId.toString());
        if (ownerEmail != null && !ownerEmail.isBlank()) {
            params.setEmail(ownerEmail);
        }
        if (restaurantName != null && !restaurantName.isBlank()) {
            params.setBusinessProfile(AccountCreateParams.BusinessProfile.builder()
                    .setName(restaurantName)
                    .build());
        }
        try {
            return stripeClient.v1().accounts().create(params.build()).getId();
        } catch (StripeException e) {
            throw new StripeConnectUnavailableException(
                    "La connexion à Stripe est temporairement indisponible. Réessayez dans quelques instants.", e);
        }
    }

    @Override
    public String createOnboardingLink(String accountId, String returnUrl, String refreshUrl) {
        AccountLinkCreateParams params = AccountLinkCreateParams.builder()
                .setAccount(accountId)
                .setReturnUrl(returnUrl)
                .setRefreshUrl(refreshUrl)
                .setType(AccountLinkCreateParams.Type.ACCOUNT_ONBOARDING)
                .build();
        try {
            return stripeClient.v1().accountLinks().create(params).getUrl();
        } catch (StripeException e) {
            throw new StripeConnectUnavailableException(
                    "La connexion à Stripe est temporairement indisponible. Réessayez dans quelques instants.", e);
        }
    }

    @Override
    public StripeConnectAccountState retrieveAccount(String accountId) {
        try {
            Account account = stripeClient.v1().accounts().retrieve(accountId);
            if (Boolean.TRUE.equals(account.getDeleted())) {
                return new StripeConnectAccountState(null, null, true);
            }
            return new StripeConnectAccountState(account.getDetailsSubmitted(), account.getPayoutsEnabled(), false);
        } catch (InvalidRequestException e) {
            // Compte inconnu de cette plateforme (supprimé ou acct_ étranger) :
            // traité comme supprimé, le champ sera nettoyé par le use case.
            return new StripeConnectAccountState(null, null, true);
        } catch (StripeException e) {
            throw new StripeConnectUnavailableException(
                    "La connexion à Stripe est temporairement indisponible. Réessayez dans quelques instants.", e);
        }
    }
}
