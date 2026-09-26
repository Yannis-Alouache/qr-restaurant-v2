package com.qrrestaurant.auth.infrastructure.persistence;

import com.qrrestaurant.auth.domain.User;
import com.qrrestaurant.auth.domain.UserRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class UserRepositoryAdapter implements UserRepository {

    private final UserJpaRepository jpaRepo;

    public UserRepositoryAdapter(UserJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public User save(User user) {
        UserJpaEntity entity = toEntity(user);
        UserJpaEntity saved = jpaRepo.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaRepo.findByEmail(email).map(this::toDomain);
    }

    @Override
    public Optional<User> findById(java.util.UUID userId) {
        return jpaRepo.findById(userId).map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaRepo.existsByEmail(email);
    }

    private User toDomain(UserJpaEntity e) {
        return User.from(e.getId(), e.getEmail(), e.getPassword(), e.getCreatedAt());
    }

    private UserJpaEntity toEntity(User d) {
        UserJpaEntity e = new UserJpaEntity();
        e.setId(d.getId());
        e.setEmail(d.getEmail());
        e.setPassword(d.getPassword());
        // Préservé lors du merge d'une entité existante (changement de mot de
        // passe) : sans copie, created_at serait écrasé à null par l'UPDATE.
        e.setCreatedAt(d.getCreatedAt());
        return e;
    }
}
