package com.MAYA.studio.controller;

import com.MAYA.studio.dto.CheckoutRequest;
import com.MAYA.studio.dto.CheckoutSessionResponse;
import com.MAYA.studio.service.CheckoutService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/checkout")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Checkout", description = "Checkout session APIs")
public class CheckoutController {

    private final CheckoutService checkoutService;

    @PostMapping("/sessions")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> createSession(@Valid @RequestBody CheckoutRequest request) {
        return ResponseEntity.ok(checkoutService.createSession(request));
    }

    @GetMapping("/sessions/{id}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<CheckoutSessionResponse> getSession(@PathVariable UUID id) {
        return ResponseEntity.ok(checkoutService.getSession(id));
    }
}
