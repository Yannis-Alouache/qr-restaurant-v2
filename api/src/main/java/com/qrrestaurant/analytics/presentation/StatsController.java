package com.qrrestaurant.analytics.presentation;

import com.qrrestaurant.analytics.application.GetRestaurantStatsUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/admin/stats")
public class StatsController {

    private final GetRestaurantStatsUseCase getRestaurantStatsUseCase;

    public StatsController(GetRestaurantStatsUseCase getRestaurantStatsUseCase) {
        this.getRestaurantStatsUseCase = getRestaurantStatsUseCase;
    }

    @GetMapping
    public ResponseEntity<GetRestaurantStatsUseCase.RestaurantStatsView> getStats(
            Authentication auth,
            @RequestParam(name = "months", defaultValue = "12") int months) {
        return ResponseEntity.ok(getRestaurantStatsUseCase.getStats(userId(auth), months));
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }
}
