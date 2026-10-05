package com.qrrestaurant.payment.connect.presentation;

import com.qrrestaurant.payment.connect.application.RefreshStripeConnectStatusUseCase;
import com.qrrestaurant.payment.connect.application.StartStripeOnboardingUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

/**
 * Flux Stripe Connect côté restaurateur : démarrage de l'onboarding Express et
 * rafraîchissement du statut au retour du formulaire Stripe.
 */
@RestController
@RequestMapping("/api/admin/connect/stripe")
public class StripeConnectController {

    private final StartStripeOnboardingUseCase startStripeOnboardingUseCase;
    private final RefreshStripeConnectStatusUseCase refreshStripeConnectStatusUseCase;

    public StripeConnectController(StartStripeOnboardingUseCase startStripeOnboardingUseCase,
                                   RefreshStripeConnectStatusUseCase refreshStripeConnectStatusUseCase) {
        this.startStripeOnboardingUseCase = startStripeOnboardingUseCase;
        this.refreshStripeConnectStatusUseCase = refreshStripeConnectStatusUseCase;
    }

    @PostMapping("/onboarding")
    public ResponseEntity<StartStripeOnboardingUseCase.OnboardingView> startOnboarding(Authentication authentication) {
        return ResponseEntity.ok(startStripeOnboardingUseCase.execute(extractUserId(authentication)));
    }

    @GetMapping("/status")
    public ResponseEntity<RefreshStripeConnectStatusUseCase.StripeConnectStatusView> getStatus(Authentication authentication) {
        return ResponseEntity.ok(refreshStripeConnectStatusUseCase.execute(extractUserId(authentication)));
    }

    private UUID extractUserId(Authentication authentication) {
        return (UUID) authentication.getPrincipal();
    }
}
