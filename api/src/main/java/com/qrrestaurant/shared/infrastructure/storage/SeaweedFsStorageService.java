package com.qrrestaurant.shared.infrastructure.storage;

import com.qrrestaurant.shared.domain.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AnonymousCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.net.URI;

@Service
public class SeaweedFsStorageService implements StorageService {

    private final S3Client s3Client;
    private final String endpoint;

    @Autowired
    public SeaweedFsStorageService(@Value("${storage.seaweedfs.s3-endpoint}") String endpoint) {
        this(
                S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.US_EAST_1)
                        .forcePathStyle(true)
                        // SeaweedFS runs without an iam identity store, so it only accepts
                        // UNSIGNED S3 requests — a signed request (even with placeholder
                        // creds) is rejected: "Signed request requires setting up SeaweedFS
                        // S3 authentication". Send anonymous (unsigned) requests for now;
                        // see #14 for adding real S3 auth.
                        .credentialsProvider(AnonymousCredentialsProvider.create())
                        .build(),
                endpoint
        );
    }

    public SeaweedFsStorageService(S3Client s3Client, String endpoint) {
        this.s3Client = s3Client;
        this.endpoint = endpoint;
    }

    @Override
    public String upload(String bucket, String key, byte[] data, String contentType) {
        ensureBucketExists(bucket);
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(contentType)
                            .build(),
                    RequestBody.fromBytes(data)
            );
            return normalizeEndpoint(endpoint) + "/" + bucket + "/" + key;
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageService.StorageUploadException("Service de stockage indisponible", exception);
        }
    }

    @Override
    public void delete(String reference) {
        if (reference == null || reference.isBlank()) {
            return;
        }
        try {
            URI uri = URI.create(reference);
            String path = uri.getPath();
            if (path == null || path.length() < 2 || path.indexOf('/', 1) < 0) {
                return; // référence non interprétable : rien à supprimer
            }
            // path = "/<bucket>/<key...>" — on découpe le premier segment puis le reste.
            String trimmed = path.substring(1); // supprime le '/' initial
            int split = trimmed.indexOf('/');
            String bucket = trimmed.substring(0, split);
            String key = trimmed.substring(split + 1);
            if (bucket.isBlank() || key.isBlank()) {
                return;
            }
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(key).build());
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageService.StorageDeleteException("Service de stockage indisponible", exception);
        }
    }

    private void ensureBucketExists(String bucket) {
        try {
            s3Client.createBucket(CreateBucketRequest.builder().bucket(bucket).build());
        } catch (BucketAlreadyExistsException | BucketAlreadyOwnedByYouException exception) {
            return;
        } catch (S3Exception exception) {
            if (exception.statusCode() == 409) {
                return;
            }
            throw new StorageService.StorageUploadException("Service de stockage indisponible", exception);
        } catch (SdkClientException exception) {
            throw new StorageService.StorageUploadException("Service de stockage indisponible", exception);
        }
    }

    private String normalizeEndpoint(String rawEndpoint) {
        if (rawEndpoint.endsWith("/")) {
            return rawEndpoint.substring(0, rawEndpoint.length() - 1);
        }
        return rawEndpoint;
    }
}
