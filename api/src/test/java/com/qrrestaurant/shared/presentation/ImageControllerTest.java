package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.restaurant.domain.Restaurant;
import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.domain.StorageService;
import com.qrrestaurant.shared.domain.UploadedFileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.security.Principal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class ImageControllerTest {

    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024;
    private static final long QUOTA_BYTES = 100 * 1024 * 1024;
    private static final byte[] PNG_BYTES = {
            (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3
    };

    private StorageService storageService;
    private RestaurantRepository restaurantRepository;
    private UploadedFileRepository uploadedFileRepository;
    private UUID ownerId;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        storageService = mock(StorageService.class);
        restaurantRepository = mock(RestaurantRepository.class);
        uploadedFileRepository = mock(UploadedFileRepository.class);
        ownerId = UUID.randomUUID();
        mockMvc = MockMvcBuilders.standaloneSetup(new ImageController(
                        storageService, restaurantRepository, uploadedFileRepository,
                        MAX_FILE_SIZE_BYTES, QUOTA_BYTES))
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        when(restaurantRepository.findByUserId(ownerId)).thenReturn(Optional.of(Restaurant.from(
                UUID.randomUUID(), ownerId, "Naia Burger", "naia-burger", null, null, null, "classique", null, null)));
    }

    @Test
    void shouldAcceptARealPngUploadAndRecordItAgainstTheQuota() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);
        when(uploadedFileRepository.totalBytesForOwner(ownerId)).thenReturn(0L);
        when(storageService.upload(eq("logos"), anyString(), any(byte[].class), eq("image/png")))
                .thenReturn("/api/images/logos/new-logo.png");

        mockMvc.perform(multipart("/api/admin/images/logos").file(image).with(authenticatedOwner()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.url").value("/api/images/logos/new-logo.png"));

        verify(uploadedFileRepository).record(any(UploadedFileRepository.UploadedFileEntry.class));
    }

    @Test
    void shouldRejectUnsupportedImageBuckets() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/admin/images/avatars").file(image).with(authenticatedOwner()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Bucket d'image non supporté"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectUploadsFromAccountsOwningNoRestaurant() throws Exception {
        UUID strangerId = UUID.randomUUID();
        when(restaurantRepository.findByUserId(strangerId)).thenReturn(Optional.empty());
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/admin/images/logos").file(image)
                        .with(ownerPrincipal(strangerId)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Seul un compte restaurateur peut téléverser des images"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectUnauthenticatedUploads() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/admin/images/logos").file(image))
                .andExpect(status().isForbidden());

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectFilesThatAreNotImagesWhateverTheDeclaredType() throws Exception {
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "notes.txt",
                "image/png",
                "not-an-image".getBytes()
        );

        mockMvc.perform(multipart("/api/admin/images/logos").file(textFile).with(authenticatedOwner()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le fichier doit être une image (JPEG, PNG, GIF ou WebP)"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectAnImageWhoseDeclaredTypeDoesNotMatchItsContent() throws Exception {
        MockMultipartFile lyingFile = new MockMultipartFile("file", "image.gif", "image/gif", PNG_BYTES);

        mockMvc.perform(multipart("/api/admin/images/logos").file(lyingFile).with(authenticatedOwner()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Le type d'image déclaré ne correspond pas au contenu du fichier"));

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectFilesAboveTheConfiguredSizeLimit() throws Exception {
        MockMultipartFile oversized = new MockMultipartFile("file", "logo.png", "image/png", new byte[6 * 1024 * 1024]);

        mockMvc.perform(multipart("/api/admin/images/logos").file(oversized).with(authenticatedOwner()))
                .andExpect(status().isPayloadTooLarge());

        verifyNoInteractions(storageService);
    }

    @Test
    void shouldRejectUploadsBeyondTheOwnerQuota() throws Exception {
        when(uploadedFileRepository.totalBytesForOwner(ownerId)).thenReturn(QUOTA_BYTES);
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);

        mockMvc.perform(multipart("/api/admin/images/logos").file(image).with(authenticatedOwner()))
                .andExpect(status().isPayloadTooLarge())
                .andExpect(jsonPath("$.message").value("Quota de stockage atteint : supprimez des images avant d'en téléverser de nouvelles"));

        verify(storageService, never()).upload(anyString(), anyString(), any(byte[].class), anyString());
    }

    @Test
    void shouldExposeExplicitErrorWhenStorageUploadFails() throws Exception {
        MockMultipartFile image = new MockMultipartFile("file", "logo.png", "image/png", PNG_BYTES);
        when(uploadedFileRepository.totalBytesForOwner(ownerId)).thenReturn(0L);
        when(storageService.upload(eq("logos"), anyString(), any(byte[].class), eq("image/png")))
                .thenThrow(new StorageService.StorageUploadException("Service de stockage indisponible"));

        mockMvc.perform(multipart("/api/admin/images/logos").file(image).with(authenticatedOwner()))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.message").value("Service de stockage indisponible"));

        verify(uploadedFileRepository, never()).record(any(UploadedFileRepository.UploadedFileEntry.class));
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

    private org.springframework.test.web.servlet.request.RequestPostProcessor authenticatedOwner() {
        return ownerPrincipal(ownerId);
    }

    /** Simule l'Authentication posé par JwtAuthenticationFilter (principal = userId). */
    private org.springframework.test.web.servlet.request.RequestPostProcessor ownerPrincipal(UUID userId) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(userId, null, List.of());
        return request -> {
            request.setUserPrincipal(auth);
            return request;
        };
    }
}
