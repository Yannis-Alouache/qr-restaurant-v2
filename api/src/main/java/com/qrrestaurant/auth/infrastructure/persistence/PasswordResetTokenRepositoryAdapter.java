package com.qrrestaurant.auth.infrastructure.persistence;

import com.qrrestaurant.auth.domain.PasswordResetToken;
import com.qrrestaurant.auth.domain.PasswordResetTokenRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class PasswordResetTokenRepositoryAdapter implements PasswordResetTokenRepository {

    private final PasswordResetTokenJpaRepository jpaRepo;

    public PasswordResetTokenRepositoryAdapter(PasswordResetTokenJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public PasswordResetToken save(PasswordResetToken token) {
        PasswordResetTokenJpaEntity saved = jpaRepo.save(PasswordResetTokenJpaEntity.fromDomain(token));
        return saved.toDomain();
    }

    @Override
    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return jpaRepo.findByTokenHash(tokenHash).map(PasswordResetTokenJpaEntity::toDomain);
    }

    @Override
    public void deleteAllByUserId(java.util.UUID userId) {
        jpaRepo.deleteByUserId(userId);
    }
}
