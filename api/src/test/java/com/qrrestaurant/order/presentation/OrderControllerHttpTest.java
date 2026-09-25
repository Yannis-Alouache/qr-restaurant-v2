package com.qrrestaurant.order.presentation;

import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class OrderControllerHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    /**
     * La classe partage la base Postgres avec les autres tests HTTP, qui
     * commitent leurs données : MenuControllerHttpTest désactive par exemple
     * des articles seedés sans les restaurer. On remet les articles du menu
     * seedé disponibles avant chaque scénario — le test ne dépend plus de
     * l'ordre d'exécution des classes.
     */
    @BeforeEach
    void restoreSeedMenuAvailability() {
        jdbcTemplate.update(
                "UPDATE menu_item SET available = true WHERE id IN (?, ?, ?, ?, ?, ?)",
                BURGER_MENU_ID, BACON_MENU_ID, FRIES_ID, NUGGETS_ID, COKE_ID, BROWNIE_ID);
    }

    @Test
    void shouldCreateOrderOnCriticalPublicHttpBoundary() throws Exception {
        UUID comboGroupId = UUID.randomUUID();

        mockMvc.perform(post("/api/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "naia-burger",
                                  "tableId": "%s",
                                  "items": [
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 2,
                                      "menuGroupId": "%s",
                                      "menuRole": "plat"
                                    },
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 2,
                                      "menuGroupId": "%s",
                                      "menuRole": "accompagnement"
                                    },
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 2,
                                      "menuGroupId": "%s",
                                      "menuRole": "boisson"
                                    },
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(TABLE_1_ID, BURGER_MENU_ID, comboGroupId, FRIES_ID, comboGroupId, COKE_ID, comboGroupId, BROWNIE_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("en_attente_paiement"))
                .andExpect(jsonPath("$.total").value(25.30));
    }

    @Test
    void shouldRejectStandaloneMenuVariantOrdersOnPublicBoundary() throws Exception {
        mockMvc.perform(post("/api/public/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "slug": "naia-burger",
                                  "tableId": "%s",
                                  "items": [
                                    {
                                      "menuItemId": "%s",
                                      "quantity": 1
                                    }
                                  ]
                                }
                                """.formatted(TABLE_1_ID, BURGER_MENU_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Composition de commande invalide"));
    }
}
