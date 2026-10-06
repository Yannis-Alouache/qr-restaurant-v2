package com.qrrestaurant.auth.infrastructure.oauth;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.InMemoryClientRegistrationRepository;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.util.StringUtils;

/**
 * Client OAuth2 Google, enregistré uniquement quand un client-id est
 * configuré : sans clé, aucun bean ni endpoint Google n'existe, et le
 * back-office masque le bouton (via {@code GET /api/auth/providers}).
 *
 * <p>La registration est construite à la main plutôt que via les propriétés
 * {@code spring.security.oauth2.client.*} : Spring Boot créerait sinon une
 * registration vide (client-id blanc) même sans configuration.</p>
 */
@Configuration
public class GoogleOAuthClientConfig {

    @Bean
    @Conditional(GoogleOAuthClientConfig.GoogleClientIdPresent.class)
    public ClientRegistrationRepository googleClientRegistrationRepository(GoogleOAuthProperties properties) {
        ClientRegistration google = ClientRegistration.withRegistrationId("google")
                .clientId(properties.getClientId())
                .clientSecret(properties.getClientSecret())
                .clientAuthenticationMethod(ClientAuthenticationMethod.CLIENT_SECRET_BASIC)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri(properties.getRedirectUri())
                .scope("openid", "profile", "email")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .tokenUri("https://oauth2.googleapis.com/token")
                .userInfoUri("https://openidconnect.googleapis.com/v1/userinfo")
                .jwkSetUri("https://www.googleapis.com/oauth2/v3/certs")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .clientName("Google")
                .build();
        return new InMemoryClientRegistrationRepository(google);
    }

    /** Le bean n'existe que si {@code app.google.client-id} est non blanc. */
    static class GoogleClientIdPresent implements Condition {
        @Override
        public boolean matches(ConditionContext context, org.springframework.core.type.AnnotatedTypeMetadata metadata) {
            String clientId = context.getEnvironment().getProperty("app.google.client-id");
            return StringUtils.hasText(clientId);
        }
    }
}
