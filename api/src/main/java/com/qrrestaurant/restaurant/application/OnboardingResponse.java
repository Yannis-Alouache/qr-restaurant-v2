package com.qrrestaurant.restaurant.application;

import java.util.List;
import java.util.UUID;

public record OnboardingResponse(
    UUID id,
    String slug,
    String name,
    List<TableView> tables
) {
    public record TableView(UUID id, int number) {}
}
