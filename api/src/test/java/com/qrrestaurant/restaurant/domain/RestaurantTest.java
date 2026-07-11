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

        restaurant.update(null, null, "   ", null, null);

        assertThat(restaurant.getLogoPath()).isNull();
    }

    @Test
    void shouldKeepLogoPathWhenUpdateOmitsIt() {
        Restaurant restaurant = restaurantWithLogo("http://cdn/logos/naia.png");

        restaurant.update("Naia Burger", null, null, null, null);

        assertThat(restaurant.getLogoPath()).isEqualTo("http://cdn/logos/naia.png");
    }

    private Restaurant restaurantWithPaymentAccount(String paymentProviderAccountId) {
        return Restaurant.from(null, null, null, null, null, null, "classique", paymentProviderAccountId, null);
    }

    private Restaurant restaurantWithLogo(String logoPath) {
        return Restaurant.from(null, null, "Naia Burger", "naia-burger", null,
                logoPath, "classique", null, null);
    }
}
