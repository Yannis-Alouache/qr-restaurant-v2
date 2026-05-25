package com.qrrestaurant.auth.domain;

public class PasswordPolicy {

    static final int MIN_PASSWORD_LENGTH = 6;

    public void validate(String rawPassword) {
        if (rawPassword == null || rawPassword.length() < MIN_PASSWORD_LENGTH) {
            throw new PasswordTooShortException();
        }
    }

    public static class PasswordTooShortException extends RuntimeException {
        public PasswordTooShortException() {
            super("Le mot de passe doit contenir au moins 6 caractères");
        }
    }
}
