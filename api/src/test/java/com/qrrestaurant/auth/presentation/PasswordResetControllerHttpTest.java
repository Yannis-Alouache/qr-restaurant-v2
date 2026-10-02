package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.auth.domain.PasswordResetMailer;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class PasswordResetControllerHttpTest extends AbstractPostgresIntegrationTest {

    private static final String TOKEN_URL_PREFIX = "http://localhost:4200/reset-password?token=";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PasswordResetMailer mailer;

    @Test
    void shouldAnswerGenericMessageWithoutEmailForUnknownAccount() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "ghost-%s@example.com"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetController.FORGOT_PASSWORD_RESPONSE));

        verify(mailer, never()).sendResetEmail(anyString(), anyString());
    }

    @Test
    void shouldSendOneTimeResetLinkForExistingAccount() throws Exception {
        String email = uniqueEmail();
        signup(email);

        mockMvc.perform(forgotPassword(email))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetController.FORGOT_PASSWORD_RESPONSE));

        ArgumentCaptor<String> resetUrl = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetEmail(eq(email), resetUrl.capture());
        assertTrue(resetUrl.getValue().startsWith(TOKEN_URL_PREFIX));
        assertTrue(resetUrl.getValue().length() > TOKEN_URL_PREFIX.length() + 30,
                "le jeton du lien doit être suffisamment long (32 octets encodés)");
    }

    @Test
    void shouldRejectInvalidEmailFormat() throws Exception {
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\": \"invalid-email\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Email invalide"));

        verify(mailer, never()).sendResetEmail(anyString(), anyString());
    }

    @Test
    void shouldCompleteFullPasswordResetCycle() throws Exception {
        String email = uniqueEmail();
        signup(email);

        mockMvc.perform(forgotPassword(email)).andExpect(status().isOk());
        String token = capturedResetToken(email);

        mockMvc.perform(resetPassword(token, "NewSecret123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value(PasswordResetController.RESET_PASSWORD_RESPONSE));

        mockMvc.perform(login(email, "Secret123!"))
                .andExpect(status().isUnauthorized());
        mockMvc.perform(login(email, "NewSecret123!"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.userId").exists());
    }

    @Test
    void shouldRejectReuseOfConsumedToken() throws Exception {
        String email = uniqueEmail();
        signup(email);
        mockMvc.perform(forgotPassword(email)).andExpect(status().isOk());
        String token = capturedResetToken(email);
        mockMvc.perform(resetPassword(token, "NewSecret123!")).andExpect(status().isOk());

        mockMvc.perform(resetPassword(token, "AnotherSecret123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ce lien de réinitialisation est invalide ou expiré"));
    }

    @Test
    void shouldRejectUnknownToken() throws Exception {
        mockMvc.perform(resetPassword("not-a-real-token", "NewSecret123!"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Ce lien de réinitialisation est invalide ou expiré"));
    }

    @Test
    void shouldRejectWeakPasswordWithoutConsumingToken() throws Exception {
        String email = uniqueEmail();
        signup(email);
        mockMvc.perform(forgotPassword(email)).andExpect(status().isOk());
        String token = capturedResetToken(email);

        mockMvc.perform(resetPassword(token, "faible"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("mot de passe")));

        mockMvc.perform(resetPassword(token, "NewSecret123!"))
                .andExpect(status().isOk());
        mockMvc.perform(login(email, "NewSecret123!")).andExpect(status().isOk());
    }

    private String capturedResetToken(String email) {
        ArgumentCaptor<String> resetUrl = ArgumentCaptor.forClass(String.class);
        verify(mailer).sendResetEmail(eq(email), resetUrl.capture());
        return resetUrl.getValue().substring(TOKEN_URL_PREFIX.length());
    }

    private MvcResult signup(String email) throws Exception {
        return mockMvc.perform(post("/api/auth/signup")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email": "%s", "password": "Secret123!"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andReturn();
    }

    private MockHttpServletRequestBuilder forgotPassword(String email) {
        return post("/api/auth/forgot-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s"}
                        """.formatted(email));
    }

    private MockHttpServletRequestBuilder resetPassword(String token,
                                                                                                     String newPassword) {
        return post("/api/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"token": "%s", "newPassword": "%s"}
                        """.formatted(token, newPassword));
    }

    private MockHttpServletRequestBuilder login(String email,
                                                                                             String password) {
        return post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"email": "%s", "password": "%s"}
                        """.formatted(email, password));
    }

    private static String uniqueEmail() {
        return "user-" + UUID.randomUUID() + "@example.com";
    }
}
