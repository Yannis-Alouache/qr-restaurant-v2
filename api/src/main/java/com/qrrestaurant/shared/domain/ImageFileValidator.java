package com.qrrestaurant.shared.domain;

/**
 * Validation du contenu réel d'un upload d'image : le Content-Type déclaré par
 * le client ne suffit pas (il est sous son contrôle), on vérifie les magic
 * bytes du fichier reçu (JPEG, PNG, GIF, WebP).
 */
public final class ImageFileValidator {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
    private static final byte[] GIF87A = {'G', 'I', 'F', '8', '7', 'a'};
    private static final byte[] GIF89A = {'G', 'I', 'F', '8', '9', 'a'};

    private ImageFileValidator() {
    }

    /**
     * Vérifie que {@code data} est une image reconnue et que le type déclaré
     * est cohérent. Lève {@link InvalidImageException} (→ 400) sinon.
     */
    public static void validate(byte[] data, String declaredContentType) {
        if (data == null || data.length == 0) {
            throw new InvalidImageException("Fichier image manquant");
        }
        String detected = detectImageType(data);
        if (detected == null) {
            throw new InvalidImageException("Le fichier doit être une image (JPEG, PNG, GIF ou WebP)");
        }
        if (declaredContentType == null || !detected.equals(normalize(declaredContentType))) {
            throw new InvalidImageException("Le type d'image déclaré ne correspond pas au contenu du fichier");
        }
    }

    /**
     * Type MIME réel du contenu, ou null si aucune signature d'image connue
     * n'est reconnue.
     */
    public static String detectImageType(byte[] data) {
        if (data == null) {
            return null;
        }
        if (startsWith(data, PNG_SIGNATURE)) {
            return "image/png";
        }
        if (startsWith(data, GIF87A) || startsWith(data, GIF89A)) {
            return "image/gif";
        }
        if (data.length >= 3
                && (data[0] & 0xFF) == 0xFF && (data[1] & 0xFF) == 0xD8 && (data[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (data.length >= 12
                && data[0] == 'R' && data[1] == 'I' && data[2] == 'F' && data[3] == 'F'
                && data[8] == 'W' && data[9] == 'E' && data[10] == 'B' && data[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    /**
     * Normalise le type déclaré : les navigateurs et certaines librairies
     * émettent parfois l'alias historique "image/jpg".
     */
    private static String normalize(String contentType) {
        return "image/jpg".equalsIgnoreCase(contentType) ? "image/jpeg" : contentType.toLowerCase();
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    /** Étend {@link IllegalArgumentException} : mappé 400 par le handler global. */
    public static class InvalidImageException extends IllegalArgumentException {
        public InvalidImageException(String message) {
            super(message);
        }
    }

    /** Taille d'un fichier au-delà de la limite configurée (→ 413). */
    public static class FileSizeExceededException extends RuntimeException {
        public FileSizeExceededException(long maxBytes) {
            super("Fichier trop volumineux : la limite est de " + maxBytes / (1024 * 1024) + " Mo");
        }
    }

    /** Quota de stockage du compte dépassé (→ 413). */
    public static class ImageQuotaExceededException extends RuntimeException {
        public ImageQuotaExceededException() {
            super("Quota de stockage atteint : supprimez des images avant d'en téléverser de nouvelles");
        }
    }

    /** Le compte authentifié ne possède pas de restaurant (→ 403). */
    public static class NoOwnedRestaurantException extends RuntimeException {
        public NoOwnedRestaurantException() {
            super("Seul un compte restaurateur peut téléverser des images");
        }
    }
}
