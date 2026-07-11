package com.qrrestaurant.shared.application;

import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.Test;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class ImageCleanupTest {

    @Test
    void shouldDeleteReferenceFromStorageWhenPresent() {
        StorageService storage = mock(StorageService.class);
        ImageCleanup cleanup = new ImageCleanup(storage);

        cleanup.delete("http://localhost:8333/logos/abc.png");

        verify(storage).delete("http://localhost:8333/logos/abc.png");
    }

    @Test
    void shouldDoNothingForBlankReference() {
        StorageService storage = mock(StorageService.class);
        ImageCleanup cleanup = new ImageCleanup(storage);

        cleanup.delete(null);
        cleanup.delete("  ");

        verify(storage, never()).delete(any());
    }

    @Test
    void shouldSwallowStorageErrorsSoBusinessOperationSucceeds() {
        StorageService storage = mock(StorageService.class);
        doThrow(new StorageService.StorageDeleteException("down"))
                .when(storage).delete("http://localhost:8333/logos/abc.png");
        ImageCleanup cleanup = new ImageCleanup(storage);

        // Ne doit pas propager — best-effort.
        cleanup.delete("http://localhost:8333/logos/abc.png");

        verify(storage).delete("http://localhost:8333/logos/abc.png");
    }
}
