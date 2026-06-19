package com.qrrestaurant.auth.domain;

import java.util.regex.Pattern;

public class PasswordPolicy {

    static final int MIN_PASSWORD_LENGTH = 8;
    public static final String PASSWORD_PATTERN =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{10,}$";
    public static final String PASSWORD_REQUIREMENTS_MESSAGE =
            "Le mot de passe doit contenir au moins 8 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial";
    private static final Pattern COMPILED_PASSWORD_PATTERN = Pattern.compile(PASSWORD_PATTERN);

    public void validate(String rawPassword) {
        if (rawPassword == null || !COMPILED_PASSWORD_PATTERN.matcher(rawPassword).matches()) {
            throw new PasswordTooShortException();
        }
    }

    public static class PasswordTooShortException extends RuntimeException {
        public PasswordTooShortException() {
            super(PASSWORD_REQUIREMENTS_MESSAGE);
        }
    }
}
