package com.qrrestaurant.auth.presentation;

import com.qrrestaurant.auth.application.dto.AuthResponse;
import com.qrrestaurant.auth.application.AuthService;
import com.qrrestaurant.auth.application.dto.AuthResult;
import com.qrrestaurant.auth.domain.PasswordPolicy;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final JwtCookieFactory jwtCookieFactory;

    public AuthController(AuthService authService, JwtCookieFactory jwtCookieFactory) {
        this.authService = authService;
        this.jwtCookieFactory = jwtCookieFactory;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request,
                                               HttpServletRequest httpRequest) {
        AuthResult result = authService.signup(request.email(), request.password());
        return withAuthCookie(result, httpRequest, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request,
                                              HttpServletRequest httpRequest) {
        AuthResult result = authService.login(request.email(), request.password());
        return withAuthCookie(result, httpRequest, HttpStatus.OK);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest httpRequest) {
        ResponseCookie cookie = jwtCookieFactory.clear(httpRequest.isSecure());
        return ResponseEntity.noContent()
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .build();
    }

    private ResponseEntity<AuthResponse> withAuthCookie(AuthResult result,
                                                        HttpServletRequest httpRequest,
                                                        HttpStatus status) {
        ResponseCookie cookie = jwtCookieFactory.issue(
                result.token(), result.expiresInSeconds(), httpRequest.isSecure());
        return ResponseEntity.status(status)
                .header(HttpHeaders.SET_COOKIE, cookie.toString())
                .body(new AuthResponse(result.userId()));
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
