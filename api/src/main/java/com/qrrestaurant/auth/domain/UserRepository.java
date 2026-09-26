package com.qrrestaurant.auth.domain;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository {
    User save(User user);
    Optional<User> findByEmail(String email);
    Optional<User> findById(UUID userId);
    boolean existsByEmail(String email);
}
