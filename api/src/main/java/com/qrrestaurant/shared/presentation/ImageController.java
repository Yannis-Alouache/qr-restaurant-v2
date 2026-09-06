package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.shared.domain.StorageService;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
public class ImageController {

    private static final Set<String> SUPPORTED_BUCKETS = Set.of("logos", "category-images", "menu-images");
    private static final CacheControl IMAGE_CACHE_CONTROL = CacheControl.maxAge(Duration.ofDays(30)).cachePublic();

    private final StorageService storageService;

    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/api/admin/images/{bucket}")
    public ResponseEntity<Map<String, String>> upload(
            @PathVariable String bucket,
            @RequestParam("file") MultipartFile file) throws IOException {
        if (!SUPPORTED_BUCKETS.contains(bucket)) {
            throw new IllegalArgumentException("Bucket d'image non supporté");
        }
        if (file.isEmpty()) {
            throw new IllegalArgumentException("Fichier image manquant");
        }

        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Le fichier doit être une image");
        }

        String originalFilename = file.getOriginalFilename();
        String key = UUID.randomUUID() + "-" + (originalFilename == null || originalFilename.isBlank() ? "image" : originalFilename);
        String url = storageService.upload(bucket, key, file.getBytes(), contentType);

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
}
