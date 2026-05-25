package com.qrrestaurant.menu.presentation;

import com.qrrestaurant.auth.infrastructure.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MenuAdminControllerHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Test
    void shouldListOwnerCategories() throws Exception {
        mockMvc.perform(get("/api/admin/categories")
                        .header("Authorization", ownerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].name").value("Burgers"))
                .andExpect(jsonPath("$[0].hasMenu").value(true));
    }

    @Test
    void shouldListOwnerMenuItems() throws Exception {
        mockMvc.perform(get("/api/admin/menu-items")
                        .header("Authorization", ownerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(9))
                .andExpect(jsonPath("$[0].name").isNotEmpty())
                .andExpect(jsonPath("$[0].categoryId").isNotEmpty());
    }

    @Test
    void shouldListOwnerCompositions() throws Exception {
        mockMvc.perform(get("/api/admin/compositions")
                        .header("Authorization", ownerBearerToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4))
                .andExpect(jsonPath("$[0].compositionType").isNotEmpty())
                .andExpect(jsonPath("$[0].menuItemId").isNotEmpty());
    }

    @Test
    void shouldReturnNotFoundWhenDeletingAnUnknownComposition() throws Exception {
        mockMvc.perform(delete("/api/admin/compositions/{id}", UUID.randomUUID())
                        .header("Authorization", ownerBearerToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Composition introuvable"));
    }

    @Test
    void shouldRejectCreatingCompositionFromMenuVariants() throws Exception {
        mockMvc.perform(post("/api/admin/compositions")
                        .header("Authorization", ownerBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "compositionType": "boisson",
                                  "menuItemId": "%s",
                                  "supplementPrice": 0
                                }
                                """.formatted(BURGER_MENU_ID)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("L'article de composition doit être un article simple disponible"));
    }

    @Test
    void shouldRejectAvailabilityRequestsWithoutAvailabilityFlag() throws Exception {
        mockMvc.perform(patch("/api/admin/menu-items/{id}/availability", BROWNIE_ID)
                        .header("Authorization", ownerBearerToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Disponibilité requise"));
    }

    private String ownerBearerToken() {
        return "Bearer " + jwtService.generateToken(OWNER_ID, "owner@test.com");
    }
}
