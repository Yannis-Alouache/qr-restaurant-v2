package com.qrrestaurant.auth.infrastructure.security;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Garde-fou au démarrage du profil {@code prod} : le yml retire la valeur par
 * défaut de {@code jwt.secret} (l'app refuse déjà de démarrer sans la variable),
 * mais un secret trop court — ou copié depuis le placeholder du dépôt — passerait
 * encore. On l'interdit explicitement plutôt que de signer des tokens avec une
 * clé prévisible.
 */
@Component
@Profile("prod")
public class ProdSecurityGuard implements InitializingBean {

    // Valeur par défaut historique du yml (base64 de "this-is-a-development-...")
    // et placeholder de .env.example : ils ne doivent jamais signer des tokens en prod.
    static final String KNOWN_DEV_SECRET =
            "dGhpcy1pcy1hLWRldmVsb3BtZW50LWp3dC1zZWNyZXQta2V5LWZvci1xci1yZXN0YXVyYW50LXYy";
    static final String KNOWN_PLACEHOLDER_SECRET = "change-this-to-a-secure-random-string-in-production";

    private static final int MIN_SECRET_BYTES = 32; // 256 bits, minimum HS256

    private final String secret;
    private final boolean cookieSecure;

    public ProdSecurityGuard(@Value("${jwt.secret}") String secret,
                             @Value("${jwt.cookie.secure:true}") boolean cookieSecure) {
        this.secret = secret;
        this.cookieSecure = cookieSecure;
    }

    @Override
    public void afterPropertiesSet() {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET trop court en production : au moins " + MIN_SECRET_BYTES
                            + " octets requis (256 bits pour HS256).");
        }
        if (KNOWN_DEV_SECRET.equals(secret) || KNOWN_PLACEHOLDER_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET en production vaut un secret/placeholder de développement connu : "
                            + "générez un secret aléatoire dédié à cet environnement.");
        }
        if (!cookieSecure) {
            // Overridable volontairement (JWT_COOKIE_SECURE=false), mais jamais en silence.
            org.slf4j.LoggerFactory.getLogger(ProdSecurityGuard.class)
                    .warn("jwt.cookie.secure=false en production : le cookie d'authentification "
                            + "circulera sans HTTPS. À ne conserver que derrière un TLS terminé en amont.");
        }
    }
}
