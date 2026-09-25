package com.qrrestaurant.shared.infrastructure.persistence.uploadedfile;

import com.qrrestaurant.shared.domain.UploadedFileRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UploadedFileJpaRepository extends JpaRepository<UploadedFileJpaEntity, UUID> {

    @Query("select coalesce(sum(f.sizeBytes), 0) from UploadedFileJpaEntity f where f.ownerId = :ownerId")
    long totalBytesByOwner(@Param("ownerId") UUID ownerId);

    Optional<UploadedFileJpaEntity> findByKey(String key);
}
