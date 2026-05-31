package com.qrrestaurant.restaurant.presentation;
import com.qrrestaurant.restaurant.application.dto.OnboardingResponse;
import com.qrrestaurant.restaurant.application.dto.OnboardingRequest;

import com.qrrestaurant.restaurant.application.*;
import com.qrrestaurant.restaurant.domain.RestaurantTheme;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin")
public class RestaurantController {

    private final OnboardingUseCase onboardingUseCase;
    private final GetRestaurantUseCase getRestaurantUseCase;
    private final UpdateRestaurantUseCase updateRestaurantUseCase;

    public RestaurantController(OnboardingUseCase onboardingUseCase,
                                 GetRestaurantUseCase getRestaurantUseCase,
                                 UpdateRestaurantUseCase updateRestaurantUseCase) {
        this.onboardingUseCase = onboardingUseCase;
        this.getRestaurantUseCase = getRestaurantUseCase;
        this.updateRestaurantUseCase = updateRestaurantUseCase;
    }

    @PostMapping("/restaurants")
    public ResponseEntity<OnboardingResponse> createRestaurant(
            Authentication authentication,
            @Valid @RequestBody OnboardingRequest request) {
        UUID userId = extractUserId(authentication);
        OnboardingResponse response = onboardingUseCase.execute(
                userId, request.name(), request.tableCount(), request.themeId());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/restaurant")
    public ResponseEntity<GetRestaurantUseCase.RestaurantView> getRestaurant(Authentication auth) {
        return ResponseEntity.ok(getRestaurantUseCase.get(extractUserId(auth)));
    }

    @PutMapping("/restaurant")
    public ResponseEntity<UpdateRestaurantUseCase.RestaurantView> updateRestaurant(
            Authentication auth, @Valid @RequestBody UpdateRestaurantRequest request) {
        return ResponseEntity.ok(updateRestaurantUseCase.execute(
                extractUserId(auth), request.name(), request.address(),
                request.logoPath(), request.themeId(), request.paymentProviderAccountId()));
    }

    @GetMapping("/restaurant/tables")
    public ResponseEntity<List<GetRestaurantUseCase.TableView>> getTables(Authentication auth) {
        return ResponseEntity.ok(getRestaurantUseCase.getTables(extractUserId(auth)));
    }

    private UUID extractUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }

    public record UpdateRestaurantRequest(
            String name,
            String address,
            String logoPath,
            @Pattern(regexp = RestaurantTheme.VALIDATION_PATTERN, message = "Thème invalide") String themeId,
            String paymentProviderAccountId
    ) {}
}
