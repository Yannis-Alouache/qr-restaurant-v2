package com.qrrestaurant.auth.application;

import com.qrrestaurant.auth.application.dto.AuthSession;
import com.qrrestaurant.auth.domain.InvalidResetTokenException;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import com.qrrestaurant.auth.domain.PasswordResetMailer;
import com.qrrestaurant.auth.domain.PasswordResetToken;
import com.qrrestaurant.auth.domain.PasswordResetTokenRepository;
import com.qrrestaurant.auth.domain.ResetTokenGenerator;
import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;
import com.qrrestaurant.auth.infrastructure.persistence.InMemoryUserRepository;
import com.qrrestaurant.auth.infrastructure.security.DeterministicPasswordEncoder;
import com.qrrestaurant.auth.infrastructure.token.DeterministicTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordResetServiceTest {

    private static final String ADMIN_BASE_URL = "http://localhost:4200";
    private static final Duration TTL = Duration.ofMinutes(60);

    private final InMemoryUserRepository userRepository = new InMemoryUserRepository();
    private final InMemoryPasswordResetTokenRepository tokenRepository = new InMemoryPasswordResetTokenRepository();
    private final StubResetTokenGenerator tokenGenerator = new StubResetTokenGenerator();
    private final RecordingMailer mailer = new RecordingMailer();
    private final AuthService authService = new AuthService(
            userRepository, new DeterministicPasswordEncoder(), new DeterministicTokenService());
    private final MutableClock clock = new MutableClock(Instant.parse("2026-01-01T12:00:00Z"));

    private PasswordResetService passwordResetService;

    @BeforeEach
    void setUp() {
        passwordResetService = new PasswordResetService(
                userRepository, tokenRepository, tokenGenerator, mailer,
                new DeterministicPasswordEncoder(), ADMIN_BASE_URL, TTL, clock);
    }

    @Test
    void shouldSendResetLinkByEmailForExistingAccount() {
        authService.signup("chef@example.com", "Secret123!");

        passwordResetService.requestReset("chef@example.com");

        assertEquals(1, mailer.sent.size());
        assertEquals("chef@example.com", mailer.sent.get(0).to());
        assertEquals(ADMIN_BASE_URL + "/reset-password?token=raw-token-1", mailer.sent.get(0).resetUrl());

        PasswordResetToken stored = tokenRepository.findByTokenHash(tokenGenerator.hash("raw-token-1")).orElseThrow();
        assertEquals(userRepository.findByEmail("chef@example.com").orElseThrow().getId(), stored.getUserId());
        assertFalse(stored.isUsed());
    }

    @Test
    void shouldDoNothingForUnknownEmail() {
        passwordResetService.requestReset("ghost@example.com");

        assertEquals(0, mailer.sent.size());
        assertEquals(0, tokenRepository.count());
    }

    @Test
    void shouldReplacePreviousTokenWhenRequestingAgain() {
        authService.signup("chef@example.com", "Secret123!");

        passwordResetService.requestReset("chef@example.com");
        passwordResetService.requestReset("chef@example.com");

        assertEquals(1, tokenRepository.count());
        assertTrue(tokenRepository.findByTokenHash(tokenGenerator.hash("raw-token-1")).isEmpty());
        assertTrue(tokenRepository.findByTokenHash(tokenGenerator.hash("raw-token-2")).isPresent());
        assertEquals(2, mailer.sent.size());
    }

    @Test
    void shouldResetPasswordWithValidToken() {
        AuthSession session = authService.signup("chef@example.com", "Secret123!");
        passwordResetService.requestReset("chef@example.com");

        passwordResetService.resetPassword("raw-token-1", "NewSecret123!");

        assertEquals(session.userId(), authService.login("chef@example.com", "NewSecret123!").userId());
        assertThrows(AuthService.InvalidCredentialsException.class,
                () -> authService.login("chef@example.com", "Secret123!"));
        assertTrue(tokenRepository.findByTokenHash(tokenGenerator.hash("raw-token-1")).orElseThrow().isUsed());
    }

    @Test
    void shouldRejectExpiredToken() {
        authService.signup("chef@example.com", "Secret123!");
        passwordResetService.requestReset("chef@example.com");
        clock.advance(TTL.plusMinutes(1));

        assertThrows(InvalidResetTokenException.class,
                () -> passwordResetService.resetPassword("raw-token-1", "NewSecret123!"));
        assertEquals("encoded::Secret123!", userRepository.findByEmail("chef@example.com").orElseThrow().getPassword());
    }

    @Test
    void shouldRejectAlreadyUsedToken() {
        authService.signup("chef@example.com", "Secret123!");
        passwordResetService.requestReset("chef@example.com");
        passwordResetService.resetPassword("raw-token-1", "NewSecret123!");

        assertThrows(InvalidResetTokenException.class,
                () -> passwordResetService.resetPassword("raw-token-1", "AnotherSecret123!"));
    }

    @Test
    void shouldRejectUnknownToken() {
        assertThrows(InvalidResetTokenException.class,
                () -> passwordResetService.resetPassword("raw-token-404", "NewSecret123!"));
    }

    @Test
    void shouldRejectWeakPasswordWithoutConsumingToken() {
        authService.signup("chef@example.com", "Secret123!");
        passwordResetService.requestReset("chef@example.com");

        assertThrows(PasswordPolicy.PasswordTooShortException.class,
                () -> passwordResetService.resetPassword("raw-token-1", "faible"));

        passwordResetService.resetPassword("raw-token-1", "NewSecret123!");
        assertEquals("encoded::NewSecret123!", userRepository.findByEmail("chef@example.com").orElseThrow().getPassword());
    }

    private record SentEmail(String to, String resetUrl) {}

    private static final class RecordingMailer implements PasswordResetMailer {
        private final List<SentEmail> sent = new ArrayList<>();

        @Override
        public void sendResetEmail(String to, String resetUrl) {
            sent.add(new SentEmail(to, resetUrl));
        }
    }

    private static final class StubResetTokenGenerator implements ResetTokenGenerator {
        private int counter = 0;

        @Override
        public String generate() {
            counter++;
            return "raw-token-" + counter;
        }

        @Override
        public String hash(String rawToken) {
            return "hash::" + rawToken;
        }
    }

    private static final class InMemoryPasswordResetTokenRepository implements PasswordResetTokenRepository {
        private final Map<String, PasswordResetToken> tokensByHash = new LinkedHashMap<>();

        int count() {
            return tokensByHash.size();
        }

        @Override
        public PasswordResetToken save(PasswordResetToken token) {
            tokensByHash.put(token.getTokenHash(), copy(token));
            return token;
        }

        @Override
        public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
            PasswordResetToken token = tokensByHash.get(tokenHash);
            return token == null ? Optional.empty() : Optional.of(copy(token));
        }

        @Override
        public void deleteAllByUserId(UUID userId) {
            tokensByHash.values().removeIf(token -> token.getUserId().equals(userId));
        }

        private PasswordResetToken copy(PasswordResetToken token) {
            PasswordResetToken copy = PasswordResetToken.from(token.getId(), token.getUserId(),
                    token.getTokenHash(), token.getExpiresAt(), token.getCreatedAt(), token.getUsedAt());
            return copy;
        }
    }

    private static final class MutableClock extends Clock {
        private Instant instant;

        MutableClock(Instant start) {
            this.instant = start;
        }

        void advance(Duration duration) {
            instant = instant.plus(duration);
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Instant instant() {
            return instant;
        }
    }
}
