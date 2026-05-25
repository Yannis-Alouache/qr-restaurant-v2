package com.qrrestaurant.shared.domain;

public interface StorageService {

    String upload(String bucket, String key, byte[] data, String contentType);

    class StorageUploadException extends RuntimeException {
        public StorageUploadException(String message) {
            super(message);
        }

        public StorageUploadException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
