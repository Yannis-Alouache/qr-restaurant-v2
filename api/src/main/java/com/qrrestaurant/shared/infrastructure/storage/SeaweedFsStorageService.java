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
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.net.URI;

@Service
public class SeaweedFsStorageService implements StorageService {

    private final S3Client s3Client;

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
                        .build()
        );
    }

    public SeaweedFsStorageService(S3Client s3Client) {
        this.s3Client = s3Client;
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
            return PUBLIC_BASE_PATH + "/" + bucket + "/" + key;
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageService.StorageUploadException("Service de stockage indisponible", exception);
        }
    }

    @Override
    public StoredObject download(String reference) {
        String[] bucketAndKey = resolveBucketAndKey(reference);
        if (bucketAndKey == null) {
            throw new StorageService.StorageObjectNotFoundException("Image introuvable");
        }
        try {
            var response = s3Client.getObjectAsBytes(
                    GetObjectRequest.builder().bucket(bucketAndKey[0]).key(bucketAndKey[1]).build()
            );
            String contentType = response.response().contentType();
            return new StoredObject(response.asByteArray(), contentType == null ? "application/octet-stream" : contentType);
        } catch (NoSuchKeyException exception) {
            throw new StorageService.StorageObjectNotFoundException("Image introuvable");
        } catch (S3Exception | SdkClientException exception) {
            throw new StorageService.StorageDownloadException("Service de stockage indisponible", exception);
        }
    }

    @Override
    public void delete(String reference) {
        String[] bucketAndKey = resolveBucketAndKey(reference);
        if (bucketAndKey == null) {
            return; // référence non interprétable : rien à supprimer
        }
        try {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketAndKey[0]).key(bucketAndKey[1]).build());
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

    /**
     * Résout {@code <bucket, key>} depuis une référence stockée. Deux formats coexistent :
     * le chemin relatif actuel {@code /api/images/<bucket>/<key>} et l'URL absolue
     * historique {@code http://<host>/<bucket>/<key>} (données antérieures à la migration
     * V7) — seul le chemin compte, le host est ignoré.
     */
    private String[] resolveBucketAndKey(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }
        String path = reference.trim();
        if (path.startsWith("http://") || path.startsWith("https://")) {
            path = URI.create(path).getPath();
        }
        if (path == null || path.length() < 2 || path.indexOf('/', 1) < 0) {
            return null;
        }
        if (path.startsWith(PUBLIC_BASE_PATH + "/")) {
            path = path.substring(PUBLIC_BASE_PATH.length() + 1);
        }
        // path = "<bucket>/<key...>" — on découpe le premier segment puis le reste.
        String trimmed = path.startsWith("/") ? path.substring(1) : path;
        int split = trimmed.indexOf('/');
        String bucket = trimmed.substring(0, split);
        String key = trimmed.substring(split + 1);
        if (bucket.isBlank() || key.isBlank()) {
            return null;
        }
        return new String[]{bucket, key};
    }
}
