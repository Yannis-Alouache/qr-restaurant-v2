package com.qrrestaurant.restaurant.infrastructure.persistence.restaurant;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "restaurant",
        uniqueConstraints = @UniqueConstraint(name = "uk_restaurant_user_id", columnNames = "user_id")
)
public class RestaurantJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, unique = true)
    private String slug;

    private String address;

    @Column(name = "logo_path")
    private String logoPath;

    @Column(name = "theme_id", nullable = false)
    private String themeId = "classique";

    @Column(name = "payment_provider_account_id")
    private String paymentProviderAccountId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
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
    public void setPaymentProviderAccountId(String paymentProviderAccountId) { this.paymentProviderAccountId = paymentProviderAccountId; }

    public LocalDateTime getCreatedAt() { return createdAt; }
}
