package com.qrrestaurant.auth.application;
import com.qrrestaurant.auth.application.dto.AuthSession;

import com.qrrestaurant.auth.domain.AuthProvider;
import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.infrastructure.token.DeterministicTokenService;
import com.qrrestaurant.auth.infrastructure.security.DeterministicPasswordEncoder;
import com.qrrestaurant.auth.infrastructure.persistence.InMemoryUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthServiceTest {

    private InMemoryUserRepository userRepository;
    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        authService = new AuthService(userRepository, new DeterministicPasswordEncoder(), new DeterministicTokenService());
    }

    @Test
    void shouldSignupWithEncodedPasswordAndGeneratedToken() {
        AuthSession response = authService.signup("chef@example.com", "Secret123!");
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
        AuthSession signupResponse = authService.signup("chef@example.com", "Secret123!");

        AuthSession loginResponse = authService.login("chef@example.com", "Secret123!");

        assertEquals(signupResponse.userId(), loginResponse.userId());
        assertEquals(signupResponse.token(), loginResponse.token());
    }

    @Test
    void shouldRejectLoginWhenPasswordDoesNotMatch() {
        authService.signup("chef@example.com", "Secret123!");

        assertThrows(AuthService.InvalidCredentialsException.class,
                () -> authService.login("chef@example.com", "wrong-password"));
    }

    @Test
    void shouldCreateGoogleAccountWithoutPasswordOnFirstGoogleLogin() {
        AuthSession session = authService.loginWithGoogle("chef@gmail.com", true);

        User saved = userRepository.findByEmail("chef@gmail.com").orElseThrow();
        assertEquals(AuthProvider.GOOGLE, saved.getAuthProvider());
        assertNull(saved.getPassword());
        assertEquals(saved.getId().toString(), session.userId());
        assertEquals("token:%s:chef@gmail.com".formatted(saved.getId()), session.token());
    }

    @Test
    void shouldReuseSameAccountOnSubsequentGoogleLogins() {
        AuthSession first = authService.loginWithGoogle("chef@gmail.com", true);

        AuthSession second = authService.loginWithGoogle("chef@gmail.com", true);

        assertEquals(first.userId(), second.userId());
        assertEquals(1, userRepository.existsByEmail("chef@gmail.com") ? 1 : 0);
    }

    @Test
    void shouldLetVerifiedGoogleEmailSignIntoExistingLocalAccount() {
        AuthSession signup = authService.signup("chef@example.com", "Secret123!");

        AuthSession googleSession = authService.loginWithGoogle("chef@example.com", true);

        assertEquals(signup.userId(), googleSession.userId());
        // Le compte reste LOCAL : la connexion par mot de passe continue de fonctionner.
        User saved = userRepository.findByEmail("chef@example.com").orElseThrow();
        assertEquals(AuthProvider.LOCAL, saved.getAuthProvider());
        assertNotNull(saved.getPassword());
    }

    @Test
    void shouldRejectGoogleLoginWhenEmailIsNotVerified() {
        assertThrows(AuthService.GoogleEmailNotVerifiedException.class,
                () -> authService.loginWithGoogle("chef@gmail.com", false));
    }

    @Test
    void shouldRejectPasswordLoginForGoogleAccount() {
        authService.loginWithGoogle("chef@gmail.com", true);

        assertThrows(AuthService.GoogleAccountException.class,
                () -> authService.login("chef@gmail.com", "Secret123!"));
    }
}
