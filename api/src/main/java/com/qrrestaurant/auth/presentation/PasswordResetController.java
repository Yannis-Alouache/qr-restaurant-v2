package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.auth.application.PasswordResetService;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class PasswordResetController {

    static final String FORGOT_PASSWORD_RESPONSE =
            "Si un compte existe pour cette adresse, un email contenant un lien de réinitialisation vient d'être envoyé.";
    static final String RESET_PASSWORD_RESPONSE =
            "Votre mot de passe a été modifié. Vous pouvez dès à présent vous connecter avec votre nouveau mot de passe.";

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordResetService.requestReset(request.email());
        return ResponseEntity.ok(new MessageResponse(FORGOT_PASSWORD_RESPONSE));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.token(), request.newPassword());
        return ResponseEntity.ok(new MessageResponse(RESET_PASSWORD_RESPONSE));
    }

    public record MessageResponse(String message) {}

    public record ForgotPasswordRequest(
            @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email
    ) {}

    public record ResetPasswordRequest(
            @NotBlank(message = "Jeton de réinitialisation requis") String token,
            @NotBlank(message = "Mot de passe requis")
            @Pattern(regexp = PasswordPolicy.PASSWORD_PATTERN, message = PasswordPolicy.PASSWORD_REQUIREMENTS_MESSAGE)
            String newPassword
    ) {}
}
