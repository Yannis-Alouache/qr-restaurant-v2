package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.auth.application.dto.AuthResponse;
import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.signup(request.email(), request.password()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password()));
    }

    public record SignupRequest(
            @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Mot de passe requis")
            @Pattern(regexp = PasswordPolicy.PASSWORD_PATTERN, message = PasswordPolicy.PASSWORD_REQUIREMENTS_MESSAGE)
            String password
    ) {}

    public record LoginRequest(
            @NotBlank(message = "Email requis") @Email(message = "Email invalide") String email,
            @NotBlank(message = "Mot de passe requis") String password
    ) {}
}
