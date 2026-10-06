package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Endpoints Google activés via des identifiants factices : la redirection vers
 * Google et le traitement du callback sont testables sans le vrai Google —
 * seul l'échange final du code (serveurs Google) est hors de portée d'un test.
 */
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "app.google.client-id=test-client-id.apps.googleusercontent.com",
        "app.google.client-secret=test-client-secret",
        "app.google.redirect-uri=http://localhost:4200/api/auth/oauth2/code/google"
})
class GoogleOAuthHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldReportGoogleProviderAsEnabledWhenConfigured() throws Exception {
        mockMvc.perform(get("/api/auth/providers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.google").value(true));
    }

    @Test
    void shouldRedirectAuthorizationStartToGoogleWithStateCookie() throws Exception {
        mockMvc.perform(get("/api/auth/oauth2/authorization/google"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location", allOf(
                        containsString("https://accounts.google.com/o/oauth2/v2/auth"),
                        containsString("client_id=test-client-id.apps.googleusercontent.com"),
                        containsString("redirect_uri=http://localhost:4200/api/auth/oauth2/code/google"),
                        containsString("state="))))
                .andExpect(header().string("Set-Cookie", containsString("oauth2_auth_request=")));
    }

    @Test
    void shouldRedirectUnmatchedCallbackToAdminLoginWithError() throws Exception {
        // Code présent mais aucun état connu (cookie absent) : la danse OAuth est
        // invalide, l'utilisateur revient sur la page de login avec l'erreur.
        mockMvc.perform(get("/api/auth/oauth2/code/google")
                        .param("code", "unexpected-code")
                        .param("state", "unknown-state"))
                .andExpect(status().is3xxRedirection())
                .andExpect(header().string("Location",
                        "http://localhost:4200/login?erreur=google"));
    }
}
