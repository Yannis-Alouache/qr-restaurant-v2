package com.qrrestaurant.shared.infrastructure;

import com.qrrestaurant.shared.domain.StorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.BucketAlreadyExistsException;
import software.amazon.awssdk.services.s3.model.BucketAlreadyOwnedByYouException;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;
import software.amazon.awssdk.core.exception.SdkClientException;

import java.net.URI;

@Service
public class SeaweedFsStorageService implements StorageService {

    private final S3Client s3Client;
    private final String endpoint;

    @Autowired
    public SeaweedFsStorageService(
            @Value("${storage.seaweedfs.s3-endpoint}") String endpoint,
            @Value("${storage.seaweedfs.access-key:any}") String accessKey,
            @Value("${storage.seaweedfs.secret-key:any}") String secretKey) {
        this(
                S3Client.builder()
                        .endpointOverride(URI.create(endpoint))
                        .region(Region.US_EAST_1)
                        .forcePathStyle(true)
                        .credentialsProvider(StaticCredentialsProvider.create(
                                AwsBasicCredentials.create(accessKey, secretKey)))
                        .build(),
                endpoint
        );
    }

    SeaweedFsStorageService(S3Client s3Client, String endpoint) {
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
