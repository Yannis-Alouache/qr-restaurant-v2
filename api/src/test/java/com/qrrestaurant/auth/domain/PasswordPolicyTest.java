package com.qrrestaurant.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void shouldAcceptAPasswordWithSixCharacters() {
        assertDoesNotThrow(() -> passwordPolicy.validate("secret"));
    }

    @Test
    void shouldRejectAPasswordWithFiveCharacters() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate("short"));
    }

    @Test
    void shouldRejectANullPassword() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate(null));
    }
}
