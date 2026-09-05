package com.qrrestaurant.shared.domain;

public interface StorageService {

    /**
     * Base du chemin public sous laquelle l'API sert les objets stockés. Le contrat
     * du port est le suivant : {@link #upload} retourne un chemin relatif de la forme
     * {@code PUBLIC_BASE_PATH/<bucket>/<key>}, persistable tel quel et adressable
     * depuis n'importe quel hôte (navigateur, téléphone) via {@code GET PUBLIC_BASE_PATH/<bucket>/<key>}.
     */
    String PUBLIC_BASE_PATH = "/api/images";

    String upload(String bucket, String key, byte[] data, String contentType);

    /**
     * Retourne le contenu de l'objet référencé par {@code reference} — la valeur exacte
     * retournée par {@link #upload}.
     *
     * @throws StorageObjectNotFoundException si l'objet n'existe pas en stockage
     */
    StoredObject download(String reference);

    /**
     * Supprime du stockage l'objet référencé par {@code reference} — la valeur exacte
     * retournée par {@link #upload}. L'implémentation se charge de résoudre le bucket
     * et la clé depuis cette référence (elle en possède le format).
     */
    void delete(String reference);

    record StoredObject(byte[] content, String contentType) {
    }

    class StorageUploadException extends RuntimeException {
        public StorageUploadException(String message) {
            super(message);
        }

        public StorageUploadException(String message, Throwable cause) {
            super(message);
        }
    }

    class StorageDownloadException extends RuntimeException {
        public StorageDownloadException(String message) {
            super(message);
        }

        public StorageDownloadException(String message, Throwable cause) {
            super(message);
        }
    }

    class StorageObjectNotFoundException extends RuntimeException {
        public StorageObjectNotFoundException(String message) {
            super(message);
        }
    }

    class StorageDeleteException extends RuntimeException {
        public StorageDeleteException(String message) {
            super(message);
        }

        public StorageDeleteException(String message, Throwable cause) {
            super(message);
        }
    }
}
