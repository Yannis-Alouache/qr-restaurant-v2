package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.shared.domain.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private StorageService storageService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(storageService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    void shouldRejectUnsupportedImageBuckets() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );

        mockMvc.perform(multipart("/api/admin/images/avatars").file(image))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bucket d'image non supporté"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectNonImageUploads() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/images/logos").file(textFile))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le fichier doit être une image"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldExposeExplicitErrorWhenStorageUploadFails() throws Exception {
        MockMultipartFile image = new MockMultipartFile(
                "file",
                "logo.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        when(storageService.upload(eq("logos"), anyString(), any(byte[].class), eq("image/png")))
                .thenThrow(new StorageService.StorageUploadException("Service de stockage indisponible"));

        mockMvc.perform(multipart("/api/admin/images/logos").file(image))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Service de stockage indisponible"));
    }

    @Test
    void shouldServeStoredImageBytes() throws Exception {
        when(storageService.download("/api/images/logos/abc-logo.png"))
                .thenReturn(new StorageService.StoredObject(new byte[]{1, 2, 3}, "image/png"));

        mockMvc.perform(get("/api/images/logos/abc-logo.png"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[]{1, 2, 3}))
                .andExpect(header().string("Cache-Control", "max-age=2592000, public"));
    }

    @Test
    void shouldRejectServingImagesFromUnsupportedBuckets() throws Exception {
        mockMvc.perform(get("/api/images/avatars/abc.png"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bucket d'image non supporté"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldExposeNotFoundWhenImageIsMissing() throws Exception {
        when(storageService.download("/api/images/logos/abc-logo.png"))
                .thenThrow(new StorageService.StorageObjectNotFoundException("Image introuvable"));

        mockMvc.perform(get("/api/images/logos/abc-logo.png"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Image introuvable"));
    }
}
