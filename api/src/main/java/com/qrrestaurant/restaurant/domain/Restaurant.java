package com.qrrestaurant.restaurant.domain;

import java.time.LocalDateTime;
import java.util.UUID;

public class Restaurant {

    private UUID id;
    private UUID userId;
    private String name;
    private String slug;
    private String address;
    private String logoPath;
    private String themeId = "classique";
    private String paymentProviderAccountId;
    private LocalDateTime createdAt;

    public Restaurant() {}

    public Restaurant(UUID id, UUID userId, String name, String slug, String address,
                      String logoPath, String themeId, String paymentProviderAccountId,
                      LocalDateTime createdAt) {
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

    public void assertCanAcceptOnlinePayments() {
        if (paymentProviderAccountId == null || paymentProviderAccountId.isBlank()) {
            throw new PaymentNotConfiguredException();
        }
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getLogoPath() { return logoPath; }
    public void setLogoPath(String logoPath) { this.logoPath = logoPath; }

    public String getThemeId() { return themeId; }
    public void setThemeId(String themeId) { this.themeId = themeId; }

    public String getPaymentProviderAccountId() { return paymentProviderAccountId; }
    public void setPaymentProviderAccountId(String paymentProviderAccountId) {
        if (paymentProviderAccountId == null) {
            this.paymentProviderAccountId = null;
            return;
        }

        String normalizedAccountId = paymentProviderAccountId.trim();
        this.paymentProviderAccountId = normalizedAccountId.isEmpty() ? null : normalizedAccountId;
    }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public static class PaymentNotConfiguredException extends IllegalArgumentException {
        public PaymentNotConfiguredException() {
            super("Ce restaurant n'a pas configuré les paiements en ligne");
        }
    }
}
