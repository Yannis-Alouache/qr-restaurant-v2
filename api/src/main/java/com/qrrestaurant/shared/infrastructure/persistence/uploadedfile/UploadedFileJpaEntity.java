package com.qrrestaurant.shared.infrastructure.persistence.uploadedfile;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "uploaded_file")
public class UploadedFileJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "owner_id", nullable = false)
    private UUID ownerId;

    @Column(nullable = false)
    private String bucket;

    @Column(nullable = false, unique = true)
    private String key;

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    protected UploadedFileJpaEntity() {
    }

    public UploadedFileJpaEntity(UUID ownerId, String bucket, String key, long sizeBytes) {
        this.ownerId = ownerId;
        this.bucket = bucket;
        this.key = key;
        this.sizeBytes = sizeBytes;
    }

    public UUID getId() { return id; }
    public UUID getOwnerId() { return ownerId; }
    public String getBucket() { return bucket; }
    public String getKey() { return key; }
    public long getSizeBytes() { return sizeBytes; }
    public Instant getCreatedAt() { return createdAt; }
}
