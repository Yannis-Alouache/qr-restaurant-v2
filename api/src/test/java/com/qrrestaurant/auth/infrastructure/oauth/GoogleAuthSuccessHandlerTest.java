package com.qrrestaurant.auth.infrastructure.oauth;

import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.auth.infrastructure.persistence.InMemoryUserRepository;
import com.qrrestaurant.auth.infrastructure.security.DeterministicPasswordEncoder;
import com.qrrestaurant.auth.infrastructure.token.DeterministicTokenService;
import com.qrrestaurant.auth.presentation.JwtCookieFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GoogleAuthSuccessHandlerTest {

    private static final String ADMIN_BASE_URL = "http://localhost:4200";

    private InMemoryUserRepository userRepository;
    private GoogleAuthSuccessHandler handler;

    @BeforeEach
    void setUp() {
        userRepository = new InMemoryUserRepository();
        AuthService authService = new AuthService(userRepository, new DeterministicPasswordEncoder(), new DeterministicTokenService());
        JwtCookieFactory cookieFactory = new JwtCookieFactory(false, "Lax", 86400000);
        handler = new GoogleAuthSuccessHandler(authService, cookieFactory, ADMIN_BASE_URL);
    }

    @Test
    void shouldIssueJwtCookieAndRedirectToAdmin() throws Exception {
        MockHttpServletResponse response = callbackResponse(googleAuthentication("chef@gmail.com", true));

        assertEquals(302, response.getStatus());
        assertEquals(ADMIN_BASE_URL + "/", response.getHeader("Location"));
        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.startsWith("jwt=token:"));
        assertTrue(setCookie.contains("HttpOnly"));
    }

    @Test
    void shouldCreateTheAccountOnFirstGoogleLogin() throws Exception {
        callbackResponse(googleAuthentication("chef@gmail.com", true));

        assertTrue(userRepository.findByEmail("chef@gmail.com").isPresent());
    }

    @Test
    void shouldRedirectToLoginWithErrorWhenEmailIsNotVerified() throws Exception {
        MockHttpServletResponse response = callbackResponse(googleAuthentication("chef@gmail.com", false));

        assertEquals(302, response.getStatus());
        assertEquals(ADMIN_BASE_URL + "/login?erreur=google", response.getHeader("Location"));
        assertNull(response.getHeader("Set-Cookie"));
        assertTrue(userRepository.findByEmail("chef@gmail.com").isEmpty());
    }

    @Test
    void shouldRedirectToLoginWithErrorWhenPrincipalIsNotOidc() throws Exception {
        DefaultOAuth2User plainUser = new DefaultOAuth2User(
                List.of(new SimpleGrantedAuthority("ROLE_USER")), Map.of("email", "chef@gmail.com"), "email");
        OAuth2AuthenticationToken authentication = new OAuth2AuthenticationToken(
                plainUser, plainUser.getAuthorities(), "google");

        MockHttpServletResponse response = callbackResponse(authentication);

        assertEquals(ADMIN_BASE_URL + "/login?erreur=google", response.getHeader("Location"));
        assertNull(response.getHeader("Set-Cookie"));
    }

    private MockHttpServletResponse callbackResponse(OAuth2AuthenticationToken authentication) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/code/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        handler.onAuthenticationSuccess(request, response, authentication);
        return response;
    }

    private static OAuth2AuthenticationToken googleAuthentication(String email, boolean emailVerified) {
        Map<String, Object> claims = Map.of(
                IdTokenClaimNames.SUB, "google-sub-123",
                "email", email,
                "email_verified", emailVerified);
        OidcIdToken idToken = new OidcIdToken("id-token-value", Instant.now(), Instant.now().plusSeconds(60), claims);
        OidcUser user = new DefaultOidcUser(List.of(new SimpleGrantedAuthority("ROLE_USER")), idToken);
        return new OAuth2AuthenticationToken(user, user.getAuthorities(), "google");
    }
}
