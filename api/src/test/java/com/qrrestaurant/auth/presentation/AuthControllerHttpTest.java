package com.qrrestaurant.auth.presentation;
import jakarta.servlet.http.Cookie;

import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class AuthControllerHttpTest extends AbstractPostgresIntegrationTest {

    private static final String OWNER_EMAIL = "owner@test.com";
    private static final String OWNER_PASSWORD = "Secret123!";

    @Autowired
    private MockMvc mockMvc;

    @Test
    void shouldRejectSignupWhenEmailIsInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "invalid-email",
                                  "password": "Secret123!"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email invalide"))
                .andExpect(jsonPath("$.details.email").value("Email invalide"));
    }

    @Test
    void shouldRejectSignupWhenPasswordDoesNotMatchTheComplexityPolicy() throws Exception {
        mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@example.com",
                                  "password": "secret123"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(
                        "Le mot de passe doit contenir au moins 10 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial"))
                .andExpect(jsonPath("$.details.password").value(
                        "Le mot de passe doit contenir au moins 10 caractères, avec une majuscule, une minuscule, un chiffre et un caractère spécial"));
    }

    @Test
    void shouldIssueJwtHttpOnlyCookieOnLogin() throws Exception {
        mockMvc.perform(loginRequest())
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
                .andExpect(jsonPath("$.userId").exists())
                .andExpect(content().string(not(containsString("token"))));
    }

    @Test
    void shouldAuthenticateSubsequentRequestUsingJwtCookie() throws Exception {
        MvcResult login = mockMvc.perform(loginRequest())
                .andExpect(status().isOk())
                .andReturn();
        String jwt = extractJwt(login);

        mockMvc.perform(get("/api/admin/restaurant").cookie(new Cookie("jwt", jwt)))
                .andExpect(status().isOk());
    }

    @Test
    void shouldRejectSubsequentRequestWithoutJwtCookie() throws Exception {
        mockMvc.perform(get("/api/admin/restaurant"))
                .andExpect(status().isForbidden());
    }

    @Test
    void shouldClearJwtCookieOnLogout() throws Exception {
        mockMvc.perform(post("/api/auth/logout"))
                .andExpect(status().isNoContent())
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("jwt=")))
                .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")));
    }

    @Test
    void shouldReturnUnauthorizedWhenCredentialsAreInvalid() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "email": "owner@test.com",
                                  "password": "WrongPassword123!"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Identifiants invalides"));
    }

    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder loginRequest() {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {
                          "email": "%s",
                          "password": "%s"
                        }
                        """.formatted(OWNER_EMAIL, OWNER_PASSWORD));
    }

    private String extractJwt(MvcResult result) {
        String setCookie = result.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        int start = "jwt=".length();
        int end = setCookie.indexOf(';');
        return setCookie.substring(start, end);
    }
}
