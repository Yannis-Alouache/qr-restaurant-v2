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
    private String coverPath;
    private String themeId;
    private String paymentProviderAccountId;
    private String stripeConnectStatus;
    private String googleReviewUrl;
    private final LocalDateTime createdAt;

    private Restaurant(UUID id, UUID userId, String name, String slug, String address,
                       String logoPath, String coverPath, String themeId, String paymentProviderAccountId, LocalDateTime createdAt) {
        this(id, userId, name, slug, address, logoPath, coverPath, themeId, paymentProviderAccountId, createdAt, null, null);
    }

    private Restaurant(UUID id, UUID userId, String name, String slug, String address,
                       String logoPath, String coverPath, String themeId, String paymentProviderAccountId,
                       LocalDateTime createdAt, String stripeConnectStatus) {
        this(id, userId, name, slug, address, logoPath, coverPath, themeId, paymentProviderAccountId,
                createdAt, stripeConnectStatus, null);
    }

    private Restaurant(UUID id, UUID userId, String name, String slug, String address,
                       String logoPath, String coverPath, String themeId, String paymentProviderAccountId,
                       LocalDateTime createdAt, String stripeConnectStatus, String googleReviewUrl) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.slug = slug;
        this.address = address;
        this.logoPath = logoPath;
        this.coverPath = coverPath;
        this.themeId = themeId;
        this.paymentProviderAccountId = paymentProviderAccountId;
        this.createdAt = createdAt;
        this.stripeConnectStatus = stripeConnectStatus;
        this.googleReviewUrl = googleReviewUrl;
    }

    public static Restaurant create(UUID userId, String name, String slug, String themeId, String logoPath) {
        Objects.requireNonNull(userId, "userId");
        Objects.requireNonNull(name, "name");
        Objects.requireNonNull(slug, "slug");
        return new Restaurant(null, userId, name, slug, null, logoPath, null,
                RestaurantTheme.normalizeOrDefault(themeId), null, null);
    }

    public static Restaurant from(UUID id, UUID userId, String name, String slug, String address,
                                   String logoPath, String coverPath, String themeId, String paymentProviderAccountId, LocalDateTime createdAt) {
        return new Restaurant(id, userId, name, slug, address, logoPath, coverPath, themeId, paymentProviderAccountId, createdAt);
    }

    /** Réhydratation complète, incluant l'état Stripe Connect (persistance). */
    public static Restaurant from(UUID id, UUID userId, String name, String slug, String address,
                                   String logoPath, String coverPath, String themeId, String paymentProviderAccountId,
                                   LocalDateTime createdAt, String stripeConnectStatus) {
        return from(id, userId, name, slug, address, logoPath, coverPath, themeId,
                paymentProviderAccountId, createdAt, stripeConnectStatus, null);
    }

    /** Réhydratation complète, Stripe Connect + lien d'avis Google (persistance). */
    public static Restaurant from(UUID id, UUID userId, String name, String slug, String address,
                                   String logoPath, String coverPath, String themeId, String paymentProviderAccountId,
                                   LocalDateTime createdAt, String stripeConnectStatus, String googleReviewUrl) {
        return new Restaurant(id, userId, name, slug, address, logoPath, coverPath, themeId,
                paymentProviderAccountId, createdAt, stripeConnectStatus, googleReviewUrl);
    }

    public void update(String name, String address, String logoPath, String coverPath, String themeId, String paymentProviderAccountId) {
        update(name, address, logoPath, coverPath, themeId, paymentProviderAccountId, null);
    }

    public void update(String name, String address, String logoPath, String coverPath, String themeId,
                       String paymentProviderAccountId, String googleReviewUrl) {
        if (name != null) this.name = name;
        if (address != null) this.address = address;
        if (logoPath != null) {
            this.logoPath = logoPath.isBlank() ? null : logoPath;
        }
        if (coverPath != null) {
            this.coverPath = coverPath.isBlank() ? null : coverPath;
        }
        if (themeId != null) this.themeId = RestaurantTheme.normalizeOrDefault(themeId);
        if (paymentProviderAccountId != null) {
            this.paymentProviderAccountId = normalizePaymentProviderAccountId(paymentProviderAccountId);
        }
        if (googleReviewUrl != null) {
            this.googleReviewUrl = googleReviewUrl.isBlank() ? null : googleReviewUrl.trim();
        }
    }

    public void assertCanAcceptOnlinePayments() {
        if (paymentProviderAccountId == null || paymentProviderAccountId.isBlank()) {
            throw new PaymentNotConfiguredException();
        }
    }

    /**
     * Rattache un compte Stripe Connect créé par le flux d'onboarding : le champ
     * de paiement est rempli automatiquement (jamais saisi à la main) et le
     * statut démarre à PENDING tant que le KYC Stripe n'est pas terminé.
     */
    public void markStripeConnectPending(String accountId) {
        String normalized = normalizePaymentProviderAccountId(accountId);
        if (normalized == null) {
            throw new IllegalArgumentException("Identifiant de compte Stripe requis");
        }
        this.paymentProviderAccountId = normalized;
        this.stripeConnectStatus = StripeConnectStatus.PENDING.value();
    }

    /** Met à jour le statut d'onboarding (webhook account.updated ou rafraîchissement manuel). */
    public void updateStripeConnectStatus(StripeConnectStatus status) {
        if (paymentProviderAccountId == null || paymentProviderAccountId.isBlank()) {
            return;
        }
        this.stripeConnectStatus = status.value();
    }

    /** Déconnecte le compte Stripe (compte supprimé chez Stripe ou introuvable). */
    public void clearStripeConnection() {
        this.paymentProviderAccountId = null;
        this.stripeConnectStatus = null;
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
    public String getCoverPath() { return coverPath; }
    public String getThemeId() { return themeId; }
    public String getPaymentProviderAccountId() { return paymentProviderAccountId; }
    public String getStripeConnectStatus() { return stripeConnectStatus; }
    public String getGoogleReviewUrl() { return googleReviewUrl; }
    public LocalDateTime getCreatedAt() { return createdAt; }

    public static class PaymentNotConfiguredException extends IllegalArgumentException {
        public PaymentNotConfiguredException() {
            super("Ce restaurant n'a pas configuré les paiements en ligne");
        }
    }
}
