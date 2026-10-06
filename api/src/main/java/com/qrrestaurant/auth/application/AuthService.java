package com.qrrestaurant.auth.application;
import com.qrrestaurant.auth.application.dto.AuthSession;

import com.qrrestaurant.auth.domain.AuthProvider;
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

    public AuthSession signup(String email, String password) {
        passwordPolicy.validate(password);
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyRegisteredException();
        }

        User user = User.create(email, passwordEncoder.encode(password));
        User saved = userRepository.save(user);

        String token = tokenService.generateToken(saved.getId(), saved.getEmail());
        return new AuthSession(token, saved.getId().toString());
    }

    public AuthSession login(String email, String password) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(InvalidCredentialsException::new);

        if (user.getAuthProvider() == AuthProvider.GOOGLE) {
            throw new GoogleAccountException();
        }

        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }

        String token = tokenService.generateToken(user.getId(), user.getEmail());
        return new AuthSession(token, user.getId().toString());
    }

    /**
     * Connexion (ou création) d'un compte via Google. Google garantit que
     * l'email appartient à l'utilisateur : la correspondance d'email suffit,
     * y compris pour un compte LOCAL existant (rattachement implicite), mais
     * l'email doit avoir été vérifié chez le fournisseur.
     */
    public AuthSession loginWithGoogle(String email, boolean emailVerified) {
        if (!emailVerified) {
            throw new GoogleEmailNotVerifiedException();
        }

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> userRepository.save(User.createGoogle(email)));

        String token = tokenService.generateToken(user.getId(), user.getEmail());
        return new AuthSession(token, user.getId().toString());
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

    public static class GoogleAccountException extends RuntimeException {
        public GoogleAccountException() {
            super("Ce compte est relié à Google : utilisez le bouton « Continuer avec Google »");
        }
    }

    public static class GoogleEmailNotVerifiedException extends RuntimeException {
        public GoogleEmailNotVerifiedException() {
            super("L'adresse email Google n'est pas vérifiée");
        }
    }
}
