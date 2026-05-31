package com.qrrestaurant.menu.presentation;
import com.qrrestaurant.menu.application.dto.MenuItemView;
import com.qrrestaurant.menu.application.dto.CompositionView;
import com.qrrestaurant.menu.application.dto.CategoryView;

import com.qrrestaurant.menu.application.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class MenuAdminController {

    private final GetAdminMenuUseCase getAdminMenuUseCase;
    private final ManageCategoryUseCase categoryUseCase;
    private final ManageMenuItemUseCase menuItemUseCase;
    private final ManageCompositionUseCase compositionUseCase;

    public MenuAdminController(GetAdminMenuUseCase getAdminMenuUseCase,
                               ManageCategoryUseCase categoryUseCase,
                               ManageMenuItemUseCase menuItemUseCase,
                               ManageCompositionUseCase compositionUseCase) {
        this.getAdminMenuUseCase = getAdminMenuUseCase;
        this.categoryUseCase = categoryUseCase;
        this.menuItemUseCase = menuItemUseCase;
        this.compositionUseCase = compositionUseCase;
    }

    // --- Categories ---

    @GetMapping("/categories")
    public ResponseEntity<java.util.List<CategoryView>> getCategories(Authentication auth) {
        return ResponseEntity.ok(getAdminMenuUseCase.getCategories(userId(auth)));
    }

    @PostMapping("/categories")
    public ResponseEntity<CategoryView> createCategory(
            Authentication auth, @Valid @RequestBody CreateCategoryRequest req) {
        CategoryView view = categoryUseCase.create(userId(auth),
                req.name(), req.imagePath(), req.position(), req.hasMenu());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/categories/{id}")
    public ResponseEntity<CategoryView> updateCategory(
            Authentication auth, @PathVariable UUID id, @Valid @RequestBody UpdateCategoryRequest req) {
        CategoryView view = categoryUseCase.update(userId(auth), id,
                req.name(), req.imagePath(), req.position(), req.hasMenu());
        return ResponseEntity.ok(view);
    }

    @DeleteMapping("/categories/{id}")
    public ResponseEntity<Void> deleteCategory(Authentication auth, @PathVariable UUID id) {
        categoryUseCase.delete(userId(auth), id);
        return ResponseEntity.noContent().build();
    }

    // --- Menu Items ---

    @GetMapping("/menu-items")
    public ResponseEntity<java.util.List<MenuItemView>> getMenuItems(Authentication auth) {
        return ResponseEntity.ok(getAdminMenuUseCase.getMenuItems(userId(auth)));
    }

    @PostMapping("/menu-items")
    public ResponseEntity<MenuItemView> createMenuItem(
            Authentication auth, @Valid @RequestBody CreateMenuItemRequest req) {
        MenuItemView view = menuItemUseCase.create(userId(auth),
                req.categoryId(), req.name(), req.description(),
                req.price(), req.imagePath(), req.menuVariantOf());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @PutMapping("/menu-items/{id}")
    public ResponseEntity<MenuItemView> updateMenuItem(
            Authentication auth, @PathVariable UUID id, @Valid @RequestBody UpdateMenuItemRequest req) {
        MenuItemView view = menuItemUseCase.update(userId(auth), id,
                req.name(), req.description(), req.price(), req.imagePath(), req.menuVariantOf());
        return ResponseEntity.ok(view);
    }

    @PatchMapping("/menu-items/{id}/availability")
    public ResponseEntity<Void> setAvailability(
            Authentication auth, @PathVariable UUID id, @Valid @RequestBody AvailabilityRequest req) {
        menuItemUseCase.setAvailability(userId(auth), id, req.available());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/menu-items/{id}")
    public ResponseEntity<Void> deleteMenuItem(Authentication auth, @PathVariable UUID id) {
        menuItemUseCase.delete(userId(auth), id);
        return ResponseEntity.noContent().build();
    }

    // --- Compositions ---

    @GetMapping("/compositions")
    public ResponseEntity<java.util.List<CompositionView>> getCompositions(Authentication auth) {
        return ResponseEntity.ok(getAdminMenuUseCase.getCompositions(userId(auth)));
    }

    @PostMapping("/compositions")
    public ResponseEntity<CompositionView> createComposition(
            Authentication auth, @Valid @RequestBody CreateCompositionRequest req) {
        CompositionView view = compositionUseCase.create(userId(auth),
                req.compositionType(), req.menuItemId(), req.supplementPrice());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @DeleteMapping("/compositions/{id}")
    public ResponseEntity<Void> deleteComposition(Authentication auth, @PathVariable UUID id) {
        compositionUseCase.delete(userId(auth), id);
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    // --- Request records ---

    public record CreateCategoryRequest(
            @NotBlank String name, String imagePath, Integer position, boolean hasMenu) {}
    public record UpdateCategoryRequest(
            String name, String imagePath, Integer position, Boolean hasMenu) {}
    public record CreateMenuItemRequest(
            @NotNull UUID categoryId, @NotBlank String name, String description,
            @NotNull BigDecimal price, String imagePath, UUID menuVariantOf) {}
    public record UpdateMenuItemRequest(
            String name, String description, BigDecimal price, String imagePath, UUID menuVariantOf) {}
    public record AvailabilityRequest(@NotNull(message = "Disponibilité requise") Boolean available) {}
    public record CreateCompositionRequest(
            @NotBlank String compositionType, @NotNull UUID menuItemId, BigDecimal supplementPrice) {}
}
