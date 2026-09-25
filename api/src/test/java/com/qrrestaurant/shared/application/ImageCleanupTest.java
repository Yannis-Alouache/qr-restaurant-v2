package com.qrrestaurant.shared.application;

import com.qrrestaurant.shared.domain.StorageService;
import com.qrrestaurant.shared.domain.UploadedFileRepository;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

class ImageCleanupTest {

    @Test
    void shouldDeleteReferenceFromStorageAndFreeTheQuotaRow() {
        StorageService storage = mock(StorageService.class);
        UploadedFileRepository uploadedFileRepository = mock(UploadedFileRepository.class);
        ImageCleanup cleanup = new ImageCleanup(storage, uploadedFileRepository);

        cleanup.delete("/api/images/logos/abc.png");

        verify(storage).delete("/api/images/logos/abc.png");
        verify(uploadedFileRepository).deleteByKey("abc.png");
    }

    @Test
    void shouldDoNothingForBlankReference() {
        StorageService storage = mock(StorageService.class);
        UploadedFileRepository uploadedFileRepository = mock(UploadedFileRepository.class);
        ImageCleanup cleanup = new ImageCleanup(storage, uploadedFileRepository);

        cleanup.delete(null);
        cleanup.delete("  ");

        verify(storage, never()).delete(any());
        verifyNoInteractions(uploadedFileRepository);
    }

    @Test
    void shouldSwallowStorageErrorsSoBusinessOperationSucceeds() {
        StorageService storage = mock(StorageService.class);
        UploadedFileRepository uploadedFileRepository = mock(UploadedFileRepository.class);
        doThrow(new StorageService.StorageDeleteException("down"))
                .when(storage).delete("/api/images/logos/abc.png");
        ImageCleanup cleanup = new ImageCleanup(storage, uploadedFileRepository);

        // Ne doit pas propager — best-effort. La ligne de quota reste alors
        // volontairement en place (le fichier existe toujours dans le stockage).
        cleanup.delete("/api/images/logos/abc.png");

        verify(storage).delete("/api/images/logos/abc.png");
        verify(uploadedFileRepository, never()).deleteByKey(anyString());
    }
}
