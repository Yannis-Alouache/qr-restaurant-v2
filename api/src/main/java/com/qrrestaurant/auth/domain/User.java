package com.qrrestaurant.auth.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class User {

    private final UUID id;
    private final String email;
    private final String password;
    private final LocalDateTime createdAt;

    private User(UUID id, String email, String password, LocalDateTime createdAt) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.createdAt = createdAt;
    }

    public static User create(String email, String password) {
        Objects.requireNonNull(email, "email");
        Objects.requireNonNull(password, "password");
        return new User(null, email, password, null);
    }

    public static User from(UUID id, String email, String password, LocalDateTime createdAt) {
        return new User(id, email, password, createdAt);
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPassword() { return password; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
