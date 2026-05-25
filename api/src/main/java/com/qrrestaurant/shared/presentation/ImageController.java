package com.qrrestaurant.shared.presentation;

import com.qrrestaurant.shared.domain.StorageService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/images")
public class ImageController {

    private static final Set<String> SUPPORTED_BUCKETS = Set.of("logos", "category-images", "menu-images");

    private final StorageService storageService;

    public ImageController(StorageService storageService) {
        this.storageService = storageService;
    }

    @PostMapping("/{bucket}")
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
}
