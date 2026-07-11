package com.qrrestaurant.shared.domain;

public interface StorageService {

    String upload(String bucket, String key, byte[] data, String contentType);

    /**
     * Supprime du stockage l'objet référencé par {@code reference} — la valeur exacte
     * retournée par {@link #upload}. L'implémentation se charge de résoudre le bucket
     * et la clé depuis cette référence (elle en possède le format).
     */
    void delete(String reference);

    class StorageUploadException extends RuntimeException {
        public StorageUploadException(String message) {
            super(message);
        }

        public StorageUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    class StorageDeleteException extends RuntimeException {
        public StorageDeleteException(String message) {
            super(message);
        }

        public StorageDeleteException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
