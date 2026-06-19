package com.qrrestaurant.auth.infrastructure.persistence;

import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryUserRepository implements UserRepository {
    private final Map<String, User> usersByEmail = new LinkedHashMap<>();

    @Override
    public User save(User user) {
        UUID id = user.getId() != null ? user.getId() : UUID.randomUUID();
        LocalDateTime createdAt = user.getCreatedAt() != null ? user.getCreatedAt() : LocalDateTime.now();

        User saved = User.from(id, user.getEmail(), user.getPassword(), createdAt);
        usersByEmail.put(saved.getEmail(), saved);
        return copy(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return Optional.ofNullable(usersByEmail.get(email)).map(this::copy);
    }

    @Override
    public boolean existsByEmail(String email) {
        return usersByEmail.containsKey(email);
    }

    private User copy(User user) {
        return User.from(user.getId(), user.getEmail(), user.getPassword(), user.getCreatedAt());
    }
}
