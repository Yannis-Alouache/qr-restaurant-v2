package com.qrrestaurant.auth.infrastructure.oauth;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2AuthorizationRequest;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HttpCookieOAuth2AuthorizationRequestRepositoryTest {

    private static final String STATE = "state-1234";

    private HttpCookieOAuth2AuthorizationRequestRepository repository;

    @BeforeEach
    void setUp() {
        repository = new HttpCookieOAuth2AuthorizationRequestRepository(false, "Lax");
    }

    @Test
    void shouldRoundTripAuthorizationRequestThroughTheCookie() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/authorization/google");
        MockHttpServletResponse response = new MockHttpServletResponse();

        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        String setCookie = response.getHeader("Set-Cookie");
        assertTrue(setCookie.startsWith("oauth2_auth_request="));
        assertTrue(setCookie.contains("HttpOnly"));
        assertTrue(setCookie.contains("Max-Age=600"));

        MockHttpServletRequest callback = callbackWithCookieFrom(response);
        callback.addParameter("state", STATE);

        OAuth2AuthorizationRequest loaded = repository.loadAuthorizationRequest(callback);

        assertNotNull(loaded);
        assertEquals("google-client-id", loaded.getClientId());
        assertEquals("http://localhost:4200/api/auth/oauth2/code/google", loaded.getRedirectUri());
        assertEquals(STATE, loaded.getState());
        assertEquals("nonce-5678", loaded.getAdditionalParameters().get("nonce"));
    }

    @Test
    void shouldReturnNullWhenNoCookieIsPresent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/code/google");
        request.addParameter("state", STATE);

        assertNull(repository.loadAuthorizationRequest(request));
    }

    @Test
    void shouldRejectCookieWhenStateDoesNotMatchTheCallbackParameter() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/authorization/google");
        MockHttpServletResponse response = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest(), request, response);

        MockHttpServletRequest callback = callbackWithCookieFrom(response);
        callback.addParameter("state", "forged-state");

        assertNull(repository.loadAuthorizationRequest(callback));
    }

    @Test
    void shouldExpireCookieOnRemovalAndReturnTheStoredRequest() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/authorization/google");
        MockHttpServletResponse saved = new MockHttpServletResponse();
        repository.saveAuthorizationRequest(authorizationRequest(), request, saved);

        MockHttpServletRequest callback = callbackWithCookieFrom(saved);
        callback.addParameter("state", STATE);
        MockHttpServletResponse removalResponse = new MockHttpServletResponse();

        OAuth2AuthorizationRequest loaded = repository.removeAuthorizationRequest(callback, removalResponse);

        assertNotNull(loaded);
        String expired = removalResponse.getHeader("Set-Cookie");
        assertTrue(expired.startsWith("oauth2_auth_request="));
        assertTrue(expired.contains("Max-Age=0"));
    }

    @Test
    void shouldTreatCorruptedCookieAsAbsent() {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/auth/oauth2/code/google");
        request.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, "not-base64!!"));
        request.addParameter("state", STATE);

        assertNull(repository.loadAuthorizationRequest(request));
    }

    private OAuth2AuthorizationRequest authorizationRequest() {
        return OAuth2AuthorizationRequest.authorizationCode()
                .clientId("google-client-id")
                .state(STATE)
                .redirectUri("http://localhost:4200/api/auth/oauth2/code/google")
                .authorizationUri("https://accounts.google.com/o/oauth2/v2/auth")
                .scopes(java.util.Set.of("openid", "profile", "email"))
                .additionalParameters(Map.of("nonce", "nonce-5678"))
                .build();
    }

    private MockHttpServletRequest callbackWithCookieFrom(MockHttpServletResponse response) {
        MockHttpServletRequest callback = new MockHttpServletRequest("GET", "/api/auth/oauth2/code/google");
        String setCookie = response.getHeader("Set-Cookie");
        String value = setCookie.substring(setCookie.indexOf('=') + 1, setCookie.indexOf(';'));
        callback.setCookies(new Cookie(HttpCookieOAuth2AuthorizationRequestRepository.COOKIE_NAME, value));
        return callback;
    }
}
