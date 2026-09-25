package com.qrrestaurant.shared.domain;

import java.util.UUID;

/**
 * Suivi des images effectivement stockées par compte — support du quota
 * d'upload. L'implémentation persistante vit côté infrastructure.
 */
public interface UploadedFileRepository {

    long totalBytesForOwner(UUID ownerId);

    void record(UploadedFileEntry entry);

    void deleteByKey(String key);

    record UploadedFileEntry(UUID ownerId, String bucket, String key, long sizeBytes) {
    }
}
