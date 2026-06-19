package com.qrrestaurant.restaurant.domain;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

public class Restaurant {

    private final UUID id;
    private final UUID userId;
    private String name;
    private final String slug;
    private String address;
    private String logoPath;
    private String themeId;
    private String paymentProviderAccountId;
    private final LocalDateTime createdAt;

    private Restaurant(UUID id, UUID userId, String name, String slug, String address,
                       String logoPath, String themeId, String paymentProviderAccountId, LocalDateTime createdAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.slug = slug;
        this.address = address;
        this.logoPath = logoPath;
        this.themeId = themeId;
        this.paymentProviderAccountId = paymentProviderAccountId;
        this.createdAt = createdAt;
    }

    public static Restaurant create(UUID userId, String name, String slug, String themeId, String logoPath) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(slug, "slug");
        return new Restaurant(null, userId, name, slug, null, logoPath,
                RestaurantTheme.normalizeOrDefault(themeId), null, null);
    }

    public static Restaurant from(UUID id, UUID userId, String name, String slug, String address,
                                   String logoPath, String themeId, String paymentProviderAccountId, LocalDateTime createdAt) {
        return new Restaurant(id, userId, name, slug, address, logoPath, themeId, paymentProviderAccountId, createdAt);
    }

    public void update(String name, String address, String logoPath, String themeId, String paymentProviderAccountId) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (logoPath != null) this.logoPath = logoPath;
        if (themeId != null) this.themeId = RestaurantTheme.normalizeOrDefault(themeId);
        if (paymentProviderAccountId != null) {
            this.paymentProviderAccountId = normalizePaymentProviderAccountId(paymentProviderAccountId);
        }
    }

    public void assertCanAcceptOnlinePayments() {
        if (paymentProviderAccountId == null || paymentProviderAccountId.isBlank()) {
            throw new PaymentNotConfiguredException();
        }
    }

    private static String normalizePaymentProviderAccountId(String paymentProviderAccountId) {
        if (paymentProviderAccountId == null) {
            return null;
        }
        String normalized = paymentProviderAccountId.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public String getName() { return name; }
    public String getSlug() { return slug; }
    public String getAddress() { return address; }
    public String getLogoPath() { return logoPath; }
    public String getThemeId() { return themeId; }
    public String getPaymentProviderAccountId() { return paymentProviderAccountId; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class PaymentNotConfiguredException extends IllegalArgumentException {
        public PaymentNotConfiguredException() {
            super("Ce restaurant n'a pas configuré les paiements en ligne");
        }
    }
}
