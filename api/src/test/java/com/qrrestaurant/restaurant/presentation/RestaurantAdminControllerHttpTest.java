package com.qrrestaurant.restaurant.presentation;

import com.qrrestaurant.auth.infrastructure.security.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class RestaurantAdminControllerHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void shouldReturnNotFoundWhenOwnerHasNoRestaurant() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "owner-without-restaurant@test.com";
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                userId,
                email,
                "encoded-password");

        mockMvc.perform(get("/api/admin/restaurant")
                        .header("Authorization", bearerToken(userId, email)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Aucun restaurant trouvé"));
    }

    @Test
    void shouldRejectSecondOnboardingForTheSameOwner() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "duplicate-owner@test.com";
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                userId,
                email,
                "encoded-password");

        String payload = """
                {
                  "name": "Bistro Serein",
                  "tableCount": 4,
                  "themeId": "chaud"
                }
                """;

        mockMvc.perform(post("/api/admin/restaurants")
                        .header("Authorization", bearerToken(userId, email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.slug").value("bistro-serein"))
                .andExpect(jsonPath("$.tables.length()").value(4));

        mockMvc.perform(post("/api/admin/restaurants")
                        .header("Authorization", bearerToken(userId, email))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Cet utilisateur possède déjà un restaurant"));
    }

    @Test
    void shouldExposeConfiguredClientBaseUrlInRestaurantAdminView() throws Exception {
        jdbcTemplate.update(
                "UPDATE restaurant SET payment_provider_account_id = ? WHERE id = ?",
                "acct_seed_test",
                RESTAURANT_ID);

        mockMvc.perform(get("/api/admin/restaurant")
                        .header("Authorization", bearerToken(OWNER_ID, "owner@test.com")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(RESTAURANT_ID.toString()))
                .andExpect(jsonPath("$.slug").value("naia-burger"))
                .andExpect(jsonPath("$.paymentProviderAccountId").value("acct_seed_test"))
                .andExpect(jsonPath("$.clientBaseUrl").value("http://localhost:4300"));
    }

    @Test
    void shouldKeepConfiguredClientBaseUrlWhenUpdatingRestaurantSettings() throws Exception {
        mockMvc.perform(put("/api/admin/restaurant")
                        .header("Authorization", bearerToken(OWNER_ID, "owner@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Naia Burger & Co",
                                  "address": "18 Rue des Menus, Paris",
                                  "themeId": "nature",
                                  "paymentProviderAccountId": "acct_live_updated"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Naia Burger & Co"))
                .andExpect(jsonPath("$.address").value("18 Rue des Menus, Paris"))
                .andExpect(jsonPath("$.themeId").value("nature"))
                .andExpect(jsonPath("$.paymentProviderAccountId").value("acct_live_updated"))
                .andExpect(jsonPath("$.clientBaseUrl").value("http://localhost:4300"));
    }

    @Test
    void shouldAllowClearingPaymentConfigurationFromRestaurantSettings() throws Exception {
        mockMvc.perform(put("/api/admin/restaurant")
                        .header("Authorization", bearerToken(OWNER_ID, "owner@test.com"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "paymentProviderAccountId": "   "
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentProviderAccountId").isEmpty());
    }

    private String bearerToken(UUID userId, String email) {
        return "Bearer " + jwtService.generateToken(userId, email);
    }
}
