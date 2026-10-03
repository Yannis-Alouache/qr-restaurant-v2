package com.qrrestaurant.auth.application;

import com.qrrestaurant.auth.domain.InvalidResetTokenException;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import com.qrrestaurant.auth.domain.PasswordResetMailer;
import com.qrrestaurant.auth.domain.PasswordResetToken;
import com.qrrestaurant.auth.domain.PasswordResetTokenRepository;
import com.qrrestaurant.auth.domain.ResetTokenGenerator;
import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

@Service
@Transactional
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final ResetTokenGenerator tokenGenerator;
    private final PasswordResetMailer mailer;
    private final PasswordEncoder passwordEncoder;
    private final PasswordPolicy passwordPolicy = new PasswordPolicy();
    private final String adminResetUrlBase;
    private final Duration tokenTtl;
    private final Clock clock;

    @Autowired
    public PasswordResetService(UserRepository userRepository,
                                PasswordResetTokenRepository tokenRepository,
                                ResetTokenGenerator tokenGenerator,
                                PasswordResetMailer mailer,
                                PasswordEncoder passwordEncoder,
                                @Value("${app.admin-base-url}") String adminBaseUrl,
                                @Value("${app.password-reset.token-ttl-minutes:60}") long tokenTtlMinutes) {
        this(userRepository, tokenRepository, tokenGenerator, mailer, passwordEncoder,
                adminBaseUrl, Duration.ofMinutes(tokenTtlMinutes), Clock.systemUTC());
    }

    PasswordResetService(UserRepository userRepository,
                         PasswordResetTokenRepository tokenRepository,
                         ResetTokenGenerator tokenGenerator,
                         PasswordResetMailer mailer,
                         PasswordEncoder passwordEncoder,
                         String adminResetUrlBase,
                         Duration tokenTtl,
                         Clock clock) {
        this.userRepository = userRepository;
        this.tokenRepository = tokenRepository;
        this.tokenGenerator = tokenGenerator;
        this.mailer = mailer;
        this.passwordEncoder = passwordEncoder;
        this.adminResetUrlBase = adminResetUrlBase;
        this.tokenTtl = tokenTtl;
        this.clock = clock;
    }

    /**
     * Demande de réinitialisation. La réponse ne doit rien révéler : un email
     * inconnu suit exactement le même chemin qu'un email connu (ici : aucun
     * jeton, aucun envoi), le contrôleur renvoie de toute façon le même message.
     */
    public void requestReset(String email) {
        userRepository.findByEmail(email).ifPresent(user -> {
            tokenRepository.deleteAllByUserId(user.getId());
            String rawToken = tokenGenerator.generate();
            LocalDateTime now = now();
            PasswordResetToken token = PasswordResetToken.issue(
                    user.getId(), tokenGenerator.hash(rawToken), now, now.plus(tokenTtl));
            tokenRepository.save(token);
            mailer.sendResetEmail(user.getEmail(), buildResetUrl(rawToken));
        });
    }

    public void resetPassword(String rawToken, String newPassword) {
        passwordPolicy.validate(newPassword);

        PasswordResetToken token = tokenRepository.findByTokenHash(tokenGenerator.hash(rawToken))
                .orElseThrow(InvalidResetTokenException::new);
        token.assertUsable(now());

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(InvalidResetTokenException::new);
        userRepository.save(user.withPassword(passwordEncoder.encode(newPassword)));

        token.markUsed(now());
        tokenRepository.save(token);
    }

    private String buildResetUrl(String rawToken) {
        return adminResetUrlBase + "/reset-password?token=" + rawToken;
    }

    private LocalDateTime now() {
        return LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC);
    }
}
