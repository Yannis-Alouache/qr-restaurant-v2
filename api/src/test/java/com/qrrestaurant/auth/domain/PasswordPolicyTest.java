package com.qrrestaurant.auth.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PasswordPolicyTest {

    private final PasswordPolicy passwordPolicy = new PasswordPolicy();

    @Test
    void shouldAcceptAPasswordThatMatchesTheComplexityRequirements() {
        assertDoesNotThrow(() -> passwordPolicy.validate("Secret123!"));
    }

    @Test
    void shouldRejectAPasswordThatIsTooShort() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate("Short1!"));
    }

    @Test
    void shouldRejectAPasswordWithoutAnUppercaseLetter() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate("secret123!"));
    }

    @Test
    void shouldRejectAPasswordWithoutASpecialCharacter() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate("Secret1234"));
    }

    @Test
    void shouldRejectANullPassword() {
        assertThrows(PasswordPolicy.PasswordTooShortException.class, () -> passwordPolicy.validate(null));
    }
}
