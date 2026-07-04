package com.MAYA.studio.controller;

import com.MAYA.studio.dto.AuthRequest;
import com.MAYA.studio.dto.AuthResponse;
import com.MAYA.studio.dto.GoogleAuthRequest;
import com.MAYA.studio.dto.RegisterRequest;
import com.MAYA.studio.service.AuthService;
import com.MAYA.studio.service.GoogleAuthService;
import com.MAYA.studio.service.PasswordResetService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Authentication APIs")
public class AuthController {

    private final AuthService authService;
    private final GoogleAuthService googleAuthService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email or phone number")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/google")
    @Operation(summary = "Sign in with Google ID token")
    public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleAuthRequest request) {
        return ResponseEntity.ok(googleAuthService.authenticateWithGoogle(request.getIdToken()));
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    public ResponseEntity<Void> forgotPassword(@RequestBody IdentifierRequest request) {
        passwordResetService.requestReset(request.getIdentifier());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with token")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.getToken(), request.getNewPassword());
        return ResponseEntity.ok().build();
    }

    @Data
    static class IdentifierRequest {
        private String identifier;
    }

    @Data
    static class ResetPasswordRequest {
        private String token;
        private String newPassword;
    }
}
