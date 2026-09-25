package com.qrrestaurant.shared.infrastructure.persistence.uploadedfile;

import com.qrrestaurant.shared.domain.UploadedFileRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public class UploadedFileRepositoryAdapter implements UploadedFileRepository {

    private final UploadedFileJpaRepository jpaRepo;

    public UploadedFileRepositoryAdapter(UploadedFileJpaRepository jpaRepo) {
        this.jpaRepo = jpaRepo;
    }

    @Override
    public long totalBytesForOwner(UUID ownerId) {
        return jpaRepo.totalBytesByOwner(ownerId);
    }

    @Override
    public void record(UploadedFileEntry entry) {
        jpaRepo.save(new UploadedFileJpaEntity(entry.ownerId(), entry.bucket(), entry.key(), entry.sizeBytes()));
    }

    @Override
    public void deleteByKey(String key) {
        jpaRepo.findByKey(key).ifPresent(jpaRepo::delete);
    }
}
