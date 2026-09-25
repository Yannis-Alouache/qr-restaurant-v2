package com.qrrestaurant.shared.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImageFileValidatorTest {

    private static final byte[] JPEG = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 1, 2, 3};
    private static final byte[] PNG = {(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 1};
    private static final byte[] GIF89 = {'G', 'I', 'F', '8', '9', 'a', 1};
    private static final byte[] WEBP = {'R', 'I', 'F', 'F', 0, 0, 0, 0, 'W', 'E', 'B', 'P', 1};

    @Test
    void detectsTheFourSupportedImageTypes() {
        assertEquals("image/jpeg", ImageFileValidator.detectImageType(JPEG));
        assertEquals("image/png", ImageFileValidator.detectImageType(PNG));
        assertEquals("image/gif", ImageFileValidator.detectImageType(GIF89));
        assertEquals("image/webp", ImageFileValidator.detectImageType(WEBP));
        assertNull(ImageFileValidator.detectImageType("<html></html>".getBytes()));
        assertNull(ImageFileValidator.detectImageType(new byte[0]));
        assertNull(ImageFileValidator.detectImageType(null));
    }

    @Test
    void acceptsMatchingDeclaredTypesIncludingTheLegacyJpgAlias() {
        assertDoesNotThrow(() -> ImageFileValidator.validate(JPEG, "image/jpeg"));
        assertDoesNotThrow(() -> ImageFileValidator.validate(JPEG, "image/jpg"));
        assertDoesNotThrow(() -> ImageFileValidator.validate(PNG, "image/png"));
        assertDoesNotThrow(() -> ImageFileValidator.validate(GIF89, "image/gif"));
        assertDoesNotThrow(() -> ImageFileValidator.validate(WEBP, "image/webp"));
    }

    @Test
    void rejectsNonImageContentWhateverTheDeclaredType() {
        assertThrows(ImageFileValidator.InvalidImageException.class,
                () -> ImageFileValidator.validate("script".getBytes(), "image/png"));
        assertThrows(ImageFileValidator.InvalidImageException.class,
                () -> ImageFileValidator.validate(new byte[0], "image/png"));
    }

    @Test
    void rejectsAMismatchBetweenDeclaredAndDetectedType() {
        assertThrows(ImageFileValidator.InvalidImageException.class,
                () -> ImageFileValidator.validate(PNG, "image/gif"));
        assertThrows(ImageFileValidator.InvalidImageException.class,
                () -> ImageFileValidator.validate(PNG, null));
    }
}
