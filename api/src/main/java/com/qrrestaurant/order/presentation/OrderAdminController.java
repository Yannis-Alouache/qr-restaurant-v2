package com.qrrestaurant.order.presentation;

import com.qrrestaurant.order.application.GetRestaurantOrdersUseCase;
import com.qrrestaurant.order.application.UpdateOrderStatusUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/admin/orders")
public class OrderAdminController {

    private final GetRestaurantOrdersUseCase getOrdersUseCase;
    private final UpdateOrderStatusUseCase updateStatusUseCase;

    public OrderAdminController(GetRestaurantOrdersUseCase getOrdersUseCase,
                                 UpdateOrderStatusUseCase updateStatusUseCase) {
        this.getOrdersUseCase = getOrdersUseCase;
        this.updateStatusUseCase = updateStatusUseCase;
    }

    @GetMapping
    public ResponseEntity<List<GetRestaurantOrdersUseCase.OrderView>> getActiveOrders(Authentication auth) {
        return ResponseEntity.ok(getOrdersUseCase.getActiveOrders(userId(auth)));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<Void> updateStatus(Authentication auth,
                                              @PathVariable UUID id,
                                              @Valid @RequestBody StatusRequest request) {
        updateStatusUseCase.execute(userId(auth), id, request.status());
        return ResponseEntity.noContent().build();
    }

    private UUID userId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    public record StatusRequest(
            @NotBlank(message = "Statut requis")
            @Pattern(regexp = "nouvelle|en_preparation|prete|servie", message = "Statut invalide")
            String status
    ) {}
}
