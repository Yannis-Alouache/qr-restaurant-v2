package com.qrrestaurant.order.presentation;

import com.qrrestaurant.order.application.CreateOrderUseCase;
import com.qrrestaurant.order.application.GetOrderUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/public")
public class OrderController {

    private final CreateOrderUseCase createOrderUseCase;
    private final GetOrderUseCase getOrderUseCase;

    public OrderController(CreateOrderUseCase createOrderUseCase, GetOrderUseCase getOrderUseCase) {
        this.createOrderUseCase = createOrderUseCase;
        this.getOrderUseCase = getOrderUseCase;
    }

    @PostMapping("/orders")
    public ResponseEntity<CreateOrderUseCase.OrderResponse> createOrder(
            @Valid @RequestBody CreateOrderRequest req) {
        CreateOrderUseCase.OrderResponse response = createOrderUseCase.execute(
                req.slug(), req.tableId(), req.items().stream()
                        .map(i -> new CreateOrderUseCase.OrderItemRequest(
                                i.menuItemId(), i.quantity(), i.menuGroupId(), i.menuRole()))
                        .toList());
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/orders/{id}")
    public ResponseEntity<GetOrderUseCase.OrderDetailResponse> getOrder(@PathVariable UUID id) {
        return ResponseEntity.ok(getOrderUseCase.execute(id));
    }

    public record CreateOrderRequest(
            @NotBlank(message = "Slug du restaurant requis") String slug,
            @NotNull(message = "Table requise") UUID tableId,
            @NotEmpty(message = "Articles de commande introuvables") @Valid List<OrderItemRequest> items) {}

    public record OrderItemRequest(
            @NotNull(message = "Article requis") UUID menuItemId,
            @Positive(message = "Quantité invalide") int quantity,
            UUID menuGroupId,
            @Pattern(regexp = "plat|accompagnement|boisson", message = "Rôle de menu invalide") String menuRole) {}
}
