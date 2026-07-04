package com.MAYA.studio.controller;

import com.MAYA.studio.dto.CartResponse;
import com.MAYA.studio.service.CartService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Cart", description = "Shopping cart APIs")
public class CartController {
    
    private final CartService cartService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Add item to cart")
    public ResponseEntity<CartResponse> addToCart(
            @RequestParam UUID productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(cartService.addToCart(productId, quantity, size));
    }
    
    @PutMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Update cart item quantity")
    public ResponseEntity<CartResponse> updateCartQuantity(
            @RequestParam UUID productId,
            @RequestParam Integer quantity,
            @RequestParam(required = false) String size) {
        return ResponseEntity.ok(cartService.updateCartQuantity(productId, quantity, size));
    }
    
    @DeleteMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Remove item from cart")
    public ResponseEntity<Void> removeFromCart(
            @RequestParam UUID productId,
            @RequestParam(required = false) String size) {
        cartService.removeFromCart(productId, size);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get cart items")
    public ResponseEntity<List<CartResponse>> getCart() {
        return ResponseEntity.ok(cartService.getCart());
    }
}
