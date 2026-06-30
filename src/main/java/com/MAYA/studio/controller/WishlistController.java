package com.MAYA.studio.controller;

import com.MAYA.studio.dto.WishlistResponse;
import com.MAYA.studio.service.WishlistService;
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
@RequestMapping("/api/wishlist")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
@Tag(name = "Wishlist", description = "Wishlist management APIs")
public class WishlistController {
    
    private final WishlistService wishlistService;
    
    @PostMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Add product to wishlist")
    public ResponseEntity<WishlistResponse> addToWishlist(@RequestParam UUID productId) {
        return ResponseEntity.ok(wishlistService.addToWishlist(productId));
    }
    
    @DeleteMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Remove product from wishlist")
    public ResponseEntity<Void> removeFromWishlist(@RequestParam UUID productId) {
        wishlistService.removeFromWishlist(productId);
        return ResponseEntity.noContent().build();
    }
    
    @GetMapping
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @Operation(summary = "Get wishlist items")
    public ResponseEntity<List<WishlistResponse>> getWishlist() {
        return ResponseEntity.ok(wishlistService.getWishlist());
    }
}
