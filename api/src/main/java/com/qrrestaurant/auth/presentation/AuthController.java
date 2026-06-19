package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.auth.application.dto.AuthResponse;
import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.auth.application.dto.AuthSession;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieFactory cookieFactory;

    public AuthController(AuthService authService, JwtCookieFactory cookieFactory) {
        this.authService = authService;
        this.cookieFactory = cookieFactory;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request,
                                               HttpServletResponse response) {
        AuthSession session = authService.signup(request.email(), request.password());
        attachCookie(response, cookieFactory.session(session.token()));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new AuthResponse(session.userId()));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletResponse response) {
        AuthSession session = authService.login(request.email(), request.password());
        attachCookie(response, cookieFactory.session(session.token()));
        return ResponseEntity.ok(new AuthResponse(session.userId()));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletResponse response) {
        attachCookie(response, cookieFactory.expired());
        return ResponseEntity.noContent().build();
    }

    /** Allows a SPA (which can't read the httpOnly cookie) to discover the authenticated user. */
    @GetMapping("/me")
    public ResponseEntity<AuthResponse> me(@AuthenticationPrincipal UUID userId) {
        return ResponseEntity.ok(new AuthResponse(userId.toString()));
    }

    private void attachCookie(HttpServletResponse response, ResponseCookie cookie) {
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
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
