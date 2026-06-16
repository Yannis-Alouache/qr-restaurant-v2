package com.qrrestaurant.menu.presentation;
import jakarta.servlet.http.Cookie;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrrestaurant.auth.infrastructure.security.JwtService;
import com.qrrestaurant.support.AbstractPostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.UUID;
import java.util.stream.StreamSupport;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@AutoConfigureMockMvc
class MenuControllerHttpTest extends AbstractPostgresIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void shouldHideUnavailableItemsAndInvalidCompositionsFromPublicMenu() throws Exception {
        mockMvc.perform(patch("/api/admin/menu-items/{id}/availability", BROWNIE_ID)
                        .cookie(new Cookie("jwt", ownerJwt()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": false
                                }
                                """))
                .andExpect(status().isNoContent());

        mockMvc.perform(patch("/api/admin/menu-items/{id}/availability", FRIES_ID)
                        .cookie(new Cookie("jwt", ownerJwt()))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "available": false
                                }
                                """))
                .andExpect(status().isNoContent());

        JsonNode publicMenu = getPublicMenu("naia-burger");

        List<String> publicItemIds = publicItemIds(publicMenu);
        List<String> compositionItemIds = compositionItemIds(publicMenu);

        assertFalse(publicItemIds.contains(BROWNIE_ID.toString()));
        assertTrue(publicItemIds.contains(BURGER_MENU_ID.toString()));
        assertFalse(compositionItemIds.contains(FRIES_ID.toString()));
    }

    @Test
    void shouldExposeOnboardedRestaurantMenuThroughPublicMenuWithVariantsAvailableForComboComposition() throws Exception {
        UUID userId = UUID.randomUUID();
        String email = "phase3-public-menu@test.com";
        jdbcTemplate.update(
                "INSERT INTO app_user (id, email, password) VALUES (?, ?, ?)",
                userId,
                email,
                "encoded-password");

        String token = jwt(userId, email);
        JsonNode restaurant = postJson("/api/admin/restaurants", token, """
                {
                  "name": "Bistro Phase Trois",
                  "tableCount": 4,
                  "themeId": "chaud"
                }
                """);
        String slug = restaurant.path("slug").asText();

        String categoryId = postJson("/api/admin/categories", token, """
                {
                  "name": "Burgers",
                  "hasMenu": true
                }
                """).path("id").asText();

        String baseItemId = postJson("/api/admin/menu-items", token, """
                {
                  "categoryId": "%s",
                  "name": "Burger signature",
                  "description": "Steak, cheddar et sauce maison",
                  "price": 13.90
                }
                """.formatted(categoryId)).path("id").asText();

        String variantId = postJson("/api/admin/menu-items", token, """
                {
                  "categoryId": "%s",
                  "name": "Menu Burger signature",
                  "price": 17.90,
                  "menuVariantOf": "%s"
                }
                """.formatted(categoryId, baseItemId)).path("id").asText();

        String sideItemId = postJson("/api/admin/menu-items", token, """
                {
                  "categoryId": "%s",
                  "name": "Frites maison",
                  "price": 4.20
                }
                """.formatted(categoryId)).path("id").asText();

        String drinkItemId = postJson("/api/admin/menu-items", token, """
                {
                  "categoryId": "%s",
                  "name": "Limonade artisanale",
                  "price": 3.10
                }
                """.formatted(categoryId)).path("id").asText();

        postJson("/api/admin/compositions", token, """
                {
                  "compositionType": "accompagnement",
                  "menuItemId": "%s",
                  "supplementPrice": 0
                }
                """.formatted(sideItemId));
        postJson("/api/admin/compositions", token, """
                {
                  "compositionType": "boisson",
                  "menuItemId": "%s",
                  "supplementPrice": 0
                }
                """.formatted(drinkItemId));

        JsonNode publicMenu = getPublicMenu(slug);

        List<String> publicItemIds = publicItemIds(publicMenu);
        List<String> compositionItemIds = compositionItemIds(publicMenu);

        assertEquals(slug, publicMenu.path("restaurant").path("slug").asText());
        assertTrue(publicItemIds.contains(baseItemId));
        assertTrue(publicItemIds.contains(variantId));
        assertTrue(publicItemIds.contains(sideItemId));
        assertTrue(publicItemIds.contains(drinkItemId));
        assertTrue(compositionItemIds.contains(sideItemId));
        assertTrue(compositionItemIds.contains(drinkItemId));
    }

    private JsonNode getPublicMenu(String slug) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/public/menu/{slug}", slug))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private JsonNode postJson(String path, String jwt, String payload) throws Exception {
        MvcResult result = mockMvc.perform(post(path)
                        .cookie(new Cookie("jwt", jwt))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }

    private List<String> publicItemIds(JsonNode publicMenu) {
        return StreamSupport.stream(publicMenu.path("categories").spliterator(), false)
                .flatMap(category -> StreamSupport.stream(category.path("items").spliterator(), false))
                .map(item -> item.path("id").asText())
                .toList();
    }

    private List<String> compositionItemIds(JsonNode publicMenu) {
        return StreamSupport.stream(publicMenu.path("compositions").spliterator(), false)
                .map(composition -> composition.path("menuItemId").asText())
                .toList();
    }

    private String ownerJwt() {
        return jwt(OWNER_ID, "owner@test.com");
    }

    private String jwt(UUID userId, String email) {
        return jwtService.generateToken(userId, email);
    }
}
