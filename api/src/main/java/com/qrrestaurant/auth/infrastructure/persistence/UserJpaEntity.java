package com.qrrestaurant.auth.infrastructure.persistence;

import com.qrrestaurant.auth.domain.AuthProvider;
import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "app_user")
public class UserJpaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String email;

    /** Null pour un compte Google — cf. V15__add_google_auth.sql. */
    @Column
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(name = "auth_provider", nullable = false)
    private AuthProvider authProvider = AuthProvider.LOCAL;

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

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public AuthProvider getAuthProvider() { return authProvider; }
    public void setAuthProvider(AuthProvider authProvider) { this.authProvider = authProvider; }

    public LocalDateTime getCreatedAt() { return createdAt; }

    /** Alimenté par l'adaptateur au merge d'une entité existante ; @PrePersist couvre la création. */
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
