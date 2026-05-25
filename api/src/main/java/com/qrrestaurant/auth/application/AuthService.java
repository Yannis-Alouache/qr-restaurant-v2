package com.qrrestaurant.auth.application;

import com.qrrestaurant.auth.domain.PasswordPolicy;
import com.qrrestaurant.auth.domain.TokenService;
import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenService tokenService;
    private final PasswordPolicy passwordPolicy;

    @Autowired
    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder, TokenService tokenService) {
        this(userRepository, passwordEncoder, tokenService, new PasswordPolicy());
    }

    AuthService(UserRepository userRepository,
                PasswordEncoder passwordEncoder,
                TokenService tokenService,
                PasswordPolicy passwordPolicy) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenService = tokenService;
        this.passwordPolicy = passwordPolicy;
    }

    public AuthResponse signup(String email, String password) {
        passwordPolicy.validate(password);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = new User();
        user.setEmail(email);
        user.setPassword(passwordEncoder.encode(password));
        User saved = userRepository.save(user);

        String token = tokenService.generateToken(saved.getId(), saved.getEmail());
        return new AuthResponse(token, saved.getId().toString());
    }

    public AuthResponse login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenService.generateToken(user.getId(), user.getEmail());
        return new AuthResponse(token, user.getId().toString());
    }

    public static class EmailAlreadyRegisteredException extends RuntimeException {
        public EmailAlreadyRegisteredException() {
            super("Cette adresse email est déjà enregistrée");
        }
    }

    public static class InvalidCredentialsException extends RuntimeException {
        public InvalidCredentialsException() {
            super("Identifiants invalides");
        }
    }
}
