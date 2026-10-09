package com.qrrestaurant.restaurant.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestaurantTest {

    @Test
    void shouldAllowOnlinePaymentsWhenAccountIsConfigured() {
        Restaurant restaurant = restaurantWithPaymentAccount("acct_test_123");

        assertDoesNotThrow(restaurant::assertCanAcceptOnlinePayments);
    }

    @Test
    void shouldRejectOnlinePaymentsWhenAccountIsMissing() {
        Restaurant restaurant = restaurantWithPaymentAccount(null);

        assertThrows(Restaurant.PaymentNotConfiguredException.class, restaurant::assertCanAcceptOnlinePayments);
    }

    @Test
    void shouldRejectOnlinePaymentsWhenAccountIsBlank() {
        Restaurant restaurant = restaurantWithPaymentAccount("   ");

        assertThrows(Restaurant.PaymentNotConfiguredException.class, restaurant::assertCanAcceptOnlinePayments);
    }

    @Test
    void shouldClearLogoPathWhenUpdateProvidesBlankValue() {
        Restaurant restaurant = restaurantWithLogo("http://cdn/logos/naia.png");

        restaurant.update(null, null, "   ", null, null, null, null);

        assertThat(restaurant.getLogoPath()).isNull();
    }

    @Test
    void shouldKeepLogoPathWhenUpdateOmitsIt() {
        Restaurant restaurant = restaurantWithLogo("http://cdn/logos/naia.png");

        restaurant.update("Naia Burger", null, null, null, null, null, null);

        assertThat(restaurant.getLogoPath()).isEqualTo("http://cdn/logos/naia.png");
    }

    @Test
    void shouldClearCoverPathWhenUpdateProvidesBlankValue() {
        Restaurant restaurant = restaurantWithCover("http://cdn/covers/naia.png");

        restaurant.update(null, null, null, "   ", null, null, null);

        assertThat(restaurant.getCoverPath()).isNull();
    }

    @Test
    void shouldKeepCoverPathWhenUpdateOmitsIt() {
        Restaurant restaurant = restaurantWithCover("http://cdn/covers/naia.png");

        restaurant.update("Naia Burger", null, null, null, null, null, null);

        assertThat(restaurant.getCoverPath()).isEqualTo("http://cdn/covers/naia.png");
    }

    @Test
    void shouldSetGoogleReviewUrlWhenUpdateProvidesIt() {
        Restaurant restaurant = restaurantWithGoogleReviewUrl(null);

        restaurant.update(null, null, null, null, null, null, "https://g.page/r/naia-burger/review");

        assertThat(restaurant.getGoogleReviewUrl()).isEqualTo("https://g.page/r/naia-burger/review");
    }

    @Test
    void shouldClearGoogleReviewUrlWhenUpdateProvidesBlankValue() {
        Restaurant restaurant = restaurantWithGoogleReviewUrl("https://g.page/r/naia-burger/review");

        restaurant.update(null, null, null, null, null, null, "   ");

        assertThat(restaurant.getGoogleReviewUrl()).isNull();
    }

    @Test
    void shouldKeepGoogleReviewUrlWhenUpdateOmitsIt() {
        Restaurant restaurant = restaurantWithGoogleReviewUrl("https://g.page/r/naia-burger/review");

        restaurant.update("Naia Burger", null, null, null, null, null, null);

        assertThat(restaurant.getGoogleReviewUrl()).isEqualTo("https://g.page/r/naia-burger/review");
    }

    @Test
    void shouldTrimGoogleReviewUrl() {
        Restaurant restaurant = restaurantWithGoogleReviewUrl(null);

        restaurant.update(null, null, null, null, null, null, "  https://g.page/r/naia-burger/review  ");

        assertThat(restaurant.getGoogleReviewUrl()).isEqualTo("https://g.page/r/naia-burger/review");
    }

    private Restaurant restaurantWithPaymentAccount(String paymentProviderAccountId) {
        return Restaurant.from(null, null, null, null, null, null, null, "classique", paymentProviderAccountId, null);
    }

    private Restaurant restaurantWithLogo(String logoPath) {
        return Restaurant.from(null, null, "Naia Burger", "naia-burger", null,
                logoPath, null, "classique", null, null);
    }

    private Restaurant restaurantWithCover(String coverPath) {
        return Restaurant.from(null, null, "Naia Burger", "naia-burger", null,
                null, coverPath, "classique", null, null);
    }

    private Restaurant restaurantWithGoogleReviewUrl(String googleReviewUrl) {
        return Restaurant.from(null, null, "Naia Burger", "naia-burger", null,
                null, null, "classique", null, null, null, googleReviewUrl);
    }
}
