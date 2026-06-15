package com.qrrestaurant.restaurant.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;

public record OnboardingRequest(
        @NotBlank(message = "Nom du restaurant requis") String name,
        @Min(value = 1, message = "Le nombre de tables doit être supérieur à 0")
        @Max(value = 50, message = "Le nombre de tables ne peut pas dépasser 50")
        int tableCount,
        @Pattern(regexp = RestaurantTheme.VALIDATION_PATTERN, message = "Thème invalide")
        String themeId,
        String logoPath
) {}
