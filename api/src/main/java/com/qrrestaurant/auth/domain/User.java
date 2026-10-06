package com.qrrestaurant.auth.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    /** Null pour un compte Google : l'authentification passe par le fournisseur. */
    private final String password;
    private final AuthProvider authProvider;
    private final LocalDateTime createdAt;

    private User(UUID id, String email, String password, AuthProvider authProvider, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.authProvider = authProvider;
        this.createdAt = createdAt;
    }

    public static User create(String email, String password) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
        return new User(null, email, password, AuthProvider.LOCAL, null);
    }

    public static User createGoogle(String email) {
        Objects.requireNonNull(email, "email");
        return new User(null, email, null, AuthProvider.GOOGLE, null);
    }

    public static User from(UUID id, String email, String password, LocalDateTime createdAt) {
        return from(id, email, password, AuthProvider.LOCAL, createdAt);
    }

    public static User from(UUID id, String email, String password, AuthProvider authProvider, LocalDateTime createdAt) {
        return new User(id, email, password, authProvider, createdAt);
    }

    public User withPassword(String newPassword) {
        Objects.requireNonNull(newPassword, "newPassword");
        return new User(id, email, newPassword, authProvider, createdAt);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public AuthProvider getAuthProvider() { return authProvider; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
