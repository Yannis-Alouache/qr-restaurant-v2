package com.qrrestaurant.auth.application;
import com.qrrestaurant.auth.application.dto.AuthResponse;

import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.infrastructure.token.DeterministicTokenService;
import com.qrrestaurant.auth.infrastructure.security.PrefixPasswordEncoder;
import com.qrrestaurant.auth.infrastructure.persistence.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private InMemoryUserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        authService = new AuthService(userRepository, new PrefixPasswordEncoder(), new DeterministicTokenService());
    }

    @Test
    void shouldSignupWithEncodedPasswordAndGeneratedToken() {
        AuthResponse response = authService.signup("chef@example.com", "Secret123!");
        User savedUser = userRepository.findByEmail("chef@example.com").orElseThrow();

        assertEquals(savedUser.getId().toString(), response.userId());
        assertEquals("encoded::Secret123!", savedUser.getPassword());
        assertEquals("token:%s:chef@example.com".formatted(savedUser.getId()), response.token());
    }

    @Test
    void shouldRejectSignupWhenEmailAlreadyExists() {
        authService.signup("chef@example.com", "Secret123!");

        assertThrows(AuthService.EmailAlreadyRegisteredException.class,
                () -> authService.signup("chef@example.com", "Another123!"));
    }

    @Test
    void shouldLoginWhenCredentialsMatchStoredPassword() {
        AuthResponse signupResponse = authService.signup("chef@example.com", "Secret123!");

        AuthResponse loginResponse = authService.login("chef@example.com", "Secret123!");

        assertEquals(signupResponse.userId(), loginResponse.userId());
        assertEquals(signupResponse.token(), loginResponse.token());
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        authService.signup("chef@example.com", "Secret123!");

        assertThrows(AuthService.InvalidCredentialsException.class,
                () -> authService.login("chef@example.com", "wrong-password"));
    }
}
