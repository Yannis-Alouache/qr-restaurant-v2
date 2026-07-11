package com.qrrestaurant.shared.infrastructure;
import com.qrrestaurant.shared.infrastructure.storage.SeaweedFsStorageService;

import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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

    @Test
    void shouldDeleteObjectByResolvingBucketAndKeyFromReferenceUrl() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");

        storageService.delete("http://localhost:8333/logos/abc-logo.png");

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("logos") && request.key().equals("abc-logo.png")));
    }

    @Test
    void shouldDeleteEvenWhenReferenceHostDiffersFromConfiguredEndpoint() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");

        // Une URL persistée avec un host de prod reste supprimable : seul le path compte.
        storageService.delete("https://cdn.example.com/category-images/cat.jpeg");

        verify(s3Client).deleteObject(argThat((DeleteObjectRequest request) ->
                request.bucket().equals("category-images") && request.key().equals("cat.jpeg")));
    }

    @Test
    void shouldNotAttemptDeletionForBlankReference() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");

        storageService.delete(null);
        storageService.delete("  ");

        verifyNoInteractions(s3Client);
    }

    @Test
    void shouldTranslateDeleteS3FailuresIntoExplicitStorageErrors() {
        S3Client s3Client = mock(S3Client.class);
        SeaweedFsStorageService storageService = new SeaweedFsStorageService(s3Client, "http://localhost:8333");
        doThrow(S3Exception.builder().statusCode(503).message("down").build())
                .when(s3Client)
                .deleteObject(any(DeleteObjectRequest.class));

        StorageService.StorageDeleteException exception = assertThrows(
                StorageService.StorageDeleteException.class,
                () -> storageService.delete("http://localhost:8333/logos/abc-logo.png")
        );

        assertEquals("Service de stockage indisponible", exception.getMessage());
    }
}
