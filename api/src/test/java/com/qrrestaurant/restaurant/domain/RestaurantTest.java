package com.qrrestaurant.restaurant.domain;

import org.junit.jupiter.api.Test;

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

    private Restaurant restaurantWithPaymentAccount(String paymentProviderAccountId) {
        return Restaurant.from(null, null, null, null, null, null, "classique", paymentProviderAccountId, null);
    }
}
