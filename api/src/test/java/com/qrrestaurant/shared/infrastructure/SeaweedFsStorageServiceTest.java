package com.qrrestaurant.shared.infrastructure;

import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class SeaweedFsStorageServiceTest {

    @Test
    void shouldCreateBucketBeforeUploadingAndReturnPublicUrl() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");

        String url = storageService.upload("logos", "logo.png", new byte[]{1, 2, 3}, "image/png");

        verify(s3Client).createBucket(argThat((CreateBucketRequest request) -> request.bucket().equals("logos")));
        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        assertEquals("http://localhost:8333/logos/logo.png", url);
    }

    @Test
    void shouldTranslateS3FailuresIntoExplicitStorageErrors() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");
        doThrow(S3Exception.builder().statusCode(503).message("down").build())
                .when(s3Client)
                .putObject(any(PutObjectRequest.class), any(RequestBody.class));

        StorageService.StorageUploadException exception = assertThrows(
                StorageService.StorageUploadException.class,
                () -> storageService.upload("logos", "logo.png", new byte[]{1, 2, 3}, "image/png")
        );

        assertEquals("Service de stockage indisponible", exception.getMessage());
    }
}
