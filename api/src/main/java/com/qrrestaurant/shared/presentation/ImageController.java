package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.restaurant.domain.RestaurantRepository;
import com.qrrestaurant.shared.domain.ImageFileValidator;
import com.qrrestaurant.shared.domain.StorageService;
import com.qrrestaurant.shared.domain.UploadedFileRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class ImageController {

    private static final Set<String> SUPPORTED_BUCKETS = Set.of("logos", "covers", "category-images", "menu-images");
    private static final CacheControl IMAGE_CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(30)).cachePublic();

    private final StorageService storageService;
    private final RestaurantRepository restaurantRepository;
    private final UploadedFileRepository uploadedFileRepository;
    private final long maxFileSizeBytes;
    private final long quotaBytesByOwner;

    public ImageController(StorageService storageService,
                           RestaurantRepository restaurantRepository,
                           UploadedFileRepository uploadedFileRepository,
                           @Value("${storage.upload.max-file-size-bytes:5242880}") long maxFileSizeBytes,
                           @Value("${storage.upload.quota-bytes-by-owner:104857600}") long quotaBytesByOwner) {
        this.storageService = storageService;
        this.restaurantRepository = restaurantRepository;
        this.uploadedFileRepository = uploadedFileRepository;
        this.maxFileSizeBytes = maxFileSizeBytes;
        this.quotaBytesByOwner = quotaBytesByOwner;
    }

    @PostMapping("/api/admin/images/{bucket}")
    public ResponseEntity<Map<String, String>> upload(
            Authentication authentication,
            @PathVariable String bucket,
            @RequestParam("file") MultipartFile file) throws IOException {
        // Upload réservé aux restaurateurs : l'identité vient du filtre JWT,
        // l'existence d'un restaurant atteste le compte propriétaire. Le quota
        // suit le compte (app_user.id), pas le restaurant.
        UUID ownerId = ownerIdOf(authentication);
        if (!SUPPORTED_BUCKETS.contains(bucket)) {
            throw new IllegalArgumentException("Bucket d'image non supporté");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier image manquant");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ImageFileValidator.FileSizeExceededException(maxFileSizeBytes);
        }

        byte[] data = file.getBytes();
        // Le Content-Type déclaré est sous contrôle du client : on valide la
        // signature réelle du fichier (magic bytes).
        ImageFileValidator.validate(data, file.getContentType());

        long usedBytes = uploadedFileRepository.totalBytesForOwner(ownerId);
        if (usedBytes + file.getSize() > quotaBytesByOwner) {
            throw new ImageFileValidator.ImageQuotaExceededException();
        }

        String originalFilename = file.getOriginalFilename();
        String safeFilename = originalFilename == null || originalFilename.isBlank()
                ? "image"
                : originalFilename.replaceAll("[^a-zA-Z0-9._-]", "_");
        String key = UUID.randomUUID() + "-" + safeFilename;
        String url = storageService.upload(bucket, key, data, file.getContentType());
        if (url == null) {
            throw new StorageService.StorageUploadException("Service de stockage indisponible");
        }
        uploadedFileRepository.record(new UploadedFileRepository.UploadedFileEntry(ownerId, bucket, key, file.getSize()));

        return ResponseEntity.ok(Map.of("url", url));
    }

    /**
     * Sert publiquement une image stockée — la référence renvoyée par l'upload est un
     * chemin relatif directement adressable par n'importe quel client (navigateur,
     * téléphone), sans connaissance de l'endpoint de stockage. Les clés étant
     * immuables (UUID), la réponse est cachable agressivement.
     */
    @GetMapping(StorageService.PUBLIC_BASE_PATH + "/{bucket}/{key}")
    public ResponseEntity<byte[]> serve(
            @PathVariable String bucket,
            @PathVariable String key) {
        if (!SUPPORTED_BUCKETS.contains(bucket)) {
            throw new IllegalArgumentException("Bucket d'image non supporté");
        }

        StorageService.StoredObject image = storageService.download(StorageService.PUBLIC_BASE_PATH + "/" + bucket + "/" + key);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.contentType()))
                .cacheControl(IMAGE_CACHE_CONTROL)
                .body(image.content());
    }

    private UUID ownerIdOf(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof UUID userId)) {
            throw new ImageFileValidator.NoOwnedRestaurantException();
        }
        // Le restaurant doit exister : un simple compte sans restaurant
        // (compte en cours d'onboarding) ne peut pas téléverser.
        restaurantRepository.findByUserId(userId)
                .orElseThrow(ImageFileValidator.NoOwnedRestaurantException::new);
        return userId;
    }
}
