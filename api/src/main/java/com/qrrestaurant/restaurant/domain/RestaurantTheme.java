package com.qrrestaurant.restaurant.domain;

import java.util.Set;

public final class RestaurantTheme {

    public static final String DEFAULT = "classique";
    public static final String VALIDATION_PATTERN = "classique|chaud|nature|elegant";
    private static final Set<String> SUPPORTED = Set.of("classique", "chaud", "nature", "elegant");

    private RestaurantTheme() {
    }

    public static String normalizeOrDefault(String themeId) {
        if (themeId == null || themeId.isBlank()) {
            return DEFAULT;
        }
        if (!SUPPORTED.contains(themeId)) {
            throw new InvalidThemeException();
        }
        return themeId;
    }

    public static class InvalidThemeException extends IllegalArgumentException {
        public InvalidThemeException() {
            super("Thème invalide");
        }
    }
}
