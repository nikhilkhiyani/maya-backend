package com.MAYA.studio.controller;

import com.MAYA.studio.dto.*;
import com.MAYA.studio.entity.ProductReview;
import com.MAYA.studio.service.ReviewService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping("/api/reviews")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> createReview(@RequestBody ReviewRequest request) {
        return ResponseEntity.ok(reviewService.createReview(request));
    }

    @GetMapping("/api/products/{productId}/reviews")
    public ResponseEntity<List<ReviewResponse>> getProductReviews(@PathVariable UUID productId) {
        return ResponseEntity.ok(reviewService.getProductReviews(productId));
    }

    @GetMapping("/api/reviews/my")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getMyReviews() {
        return ResponseEntity.ok(reviewService.getMyReviews());
    }

    @GetMapping("/api/reviews/can-review")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    public ResponseEntity<Boolean> canReview(@RequestParam UUID orderId, @RequestParam UUID productId) {
        return ResponseEntity.ok(reviewService.canReview(orderId, productId));
    }

    @GetMapping("/api/admin/reviews")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<ReviewResponse>> getPendingReviews() {
        return ResponseEntity.ok(reviewService.getPendingReviews());
    }

    @PutMapping("/api/admin/reviews/{id}/moderate")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> moderateReview(@PathVariable UUID id, @RequestBody ReviewModerateRequest request) {
        return ResponseEntity.ok(reviewService.moderateReview(id, request.getStatus(), request.getAdminReply()));
    }

    @DeleteMapping("/api/admin/reviews/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteReview(@PathVariable UUID id) {
        reviewService.deleteReview(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/api/admin/reviews/{id}/pin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> pinReview(@PathVariable UUID id, @RequestParam boolean pinned) {
        return ResponseEntity.ok(reviewService.pinReview(id, pinned));
    }

    @PutMapping("/api/admin/reviews/{id}/feature")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ReviewResponse> featureReview(@PathVariable UUID id, @RequestParam boolean featured) {
        return ResponseEntity.ok(reviewService.featureReview(id, featured));
    }
}
