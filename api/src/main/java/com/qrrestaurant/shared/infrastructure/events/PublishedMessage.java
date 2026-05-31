package com.qrrestaurant.shared.infrastructure.events;

public record PublishedMessage(String destination, Object payload) {
}
