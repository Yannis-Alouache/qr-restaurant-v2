package com.qrrestaurant.payment.presentation;

import com.qrrestaurant.payment.application.CreateCheckoutSessionUseCase;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/public/payments")
public class PaymentController {

    private final CreateCheckoutSessionUseCase createCheckoutSessionUseCase;

    public PaymentController(CreateCheckoutSessionUseCase createCheckoutSessionUseCase) {
        this.createCheckoutSessionUseCase = createCheckoutSessionUseCase;
    }

    @PostMapping("/checkout")
    public ResponseEntity<CreateCheckoutSessionUseCase.CheckoutSessionResponse> createCheckout(
            @Valid @RequestBody CheckoutRequest request) {
        CreateCheckoutSessionUseCase.CheckoutSessionResponse response =
                createCheckoutSessionUseCase.execute(request.orderId());
        return ResponseEntity.ok(response);
    }

    public record CheckoutRequest(@NotNull(message = "Commande requise") UUID orderId) {}
}
