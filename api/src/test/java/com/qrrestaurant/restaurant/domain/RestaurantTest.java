package com.qrrestaurant.restaurant.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class RestaurantTest {

    @Test
    void shouldAllowOnlinePaymentsWhenAccountIsConfigured() {
        Restaurant restaurant = new Restaurant();
        restaurant.setPaymentProviderAccountId("acct_test_123");

        assertDoesNotThrow(restaurant::assertCanAcceptOnlinePayments);
    }

    @Test
    void shouldRejectOnlinePaymentsWhenAccountIsMissing() {
        Restaurant restaurant = new Restaurant();

        assertThrows(Restaurant.PaymentNotConfiguredException.class, restaurant::assertCanAcceptOnlinePayments);
    }

    @Test
    void shouldRejectOnlinePaymentsWhenAccountIsBlank() {
        Restaurant restaurant = new Restaurant();
        restaurant.setPaymentProviderAccountId("   ");

        assertThrows(Restaurant.PaymentNotConfiguredException.class, restaurant::assertCanAcceptOnlinePayments);
    }
}
