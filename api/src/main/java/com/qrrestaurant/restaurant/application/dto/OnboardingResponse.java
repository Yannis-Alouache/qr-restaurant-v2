package com.qrrestaurant.restaurant.application.dto;

import java.util.List;
import java.util.UUID;

public record OnboardingResponse(
    UUID id,
    String slug,
    String name,
    List<TableView> tables,
    String logoPath
) {
    public record TableView(UUID id, int number) {}
}
