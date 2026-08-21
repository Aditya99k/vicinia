package com.vicinia.authservice.web;

import com.vicinia.authservice.dto.AuthResponse;
import com.vicinia.authservice.dto.ForgotPasswordRequest;
import com.vicinia.authservice.dto.LoginRequest;
import com.vicinia.authservice.dto.MessageResponse;
import com.vicinia.authservice.dto.RefreshRequest;
import com.vicinia.authservice.dto.ResetPasswordRequest;
import com.vicinia.authservice.dto.SignupRequest;
import com.vicinia.authservice.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Mapped at /api/auth to match api-gateway's route predicate exactly —
 * Stage 1's gateway config does no path rewriting (no StripPrefix filter),
 * so the full incoming path is what reaches this controller.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    public ResponseEntity<AuthResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.signup(request));
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest request) {
        return authService.refresh(request);
    }

    @PostMapping("/logout")
    public MessageResponse logout(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        String token = authorizationHeader.startsWith("Bearer ")
                ? authorizationHeader.substring(7) : authorizationHeader;
        authService.logout(token);
        return new MessageResponse("Logged out");
    }

    @PostMapping("/forgot-password")
    public MessageResponse forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request.email());
        return new MessageResponse("If that email is registered, a reset link has been sent");
    }

    @PostMapping("/reset-password")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return new MessageResponse("Password updated");
    }
}
