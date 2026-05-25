package com.qrrestaurant.menu.presentation;

import com.qrrestaurant.menu.application.GetMenuUseCase;
import com.qrrestaurant.menu.application.MenuView;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/public/menu")
public class MenuController {

    private final GetMenuUseCase getMenuUseCase;

    public MenuController(GetMenuUseCase getMenuUseCase) {
        this.getMenuUseCase = getMenuUseCase;
    }

    @GetMapping("/{slug}")
    public ResponseEntity<MenuView> getMenu(@PathVariable String slug) {
        return ResponseEntity.ok(getMenuUseCase.execute(slug));
    }
}
