package com.qrrestaurant.auth.infrastructure;

import org.springframework.security.crypto.password.PasswordEncoder;

public class PrefixPasswordEncoder implements PasswordEncoder {
    @Override
    public String encode(CharSequence rawPassword) {
        return "encoded::" + rawPassword;
    }

    @Override
    public boolean matches(CharSequence rawPassword, String encodedPassword) {
        return encodedPassword.equals(encode(rawPassword));
    }
}
