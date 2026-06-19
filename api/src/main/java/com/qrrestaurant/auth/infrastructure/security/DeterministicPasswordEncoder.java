package com.qrrestaurant.auth.infrastructure.security;

import org.springframework.security.crypto.password.PasswordEncoder;

public class DeterministicPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return "encoded::" + rawPassword;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword.equals(encode(rawPassword));
    }
}
