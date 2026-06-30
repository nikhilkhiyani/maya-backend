package com.MAYA.studio.controller;

import com.MAYA.studio.dto.PaymentResponse;
import com.MAYA.studio.dto.PaymentStatusResponse;
import com.MAYA.studio.dto.PaymentVerifyRequest;
import com.MAYA.studio.dto.PaymentWebhookRequest;
import com.MAYA.studio.dto.RazorpayOrderResponse;
import com.MAYA.studio.service.PaymentService;
import com.MAYA.studio.service.RazorpayService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment APIs")
public class PaymentController {

    private final PaymentService paymentService;
    private final RazorpayService razorpayService;

    @PostMapping("/upi/initiate/{checkoutSessionId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentResponse> initiateUpiPayment(@PathVariable UUID checkoutSessionId) {
        return ResponseEntity.ok(paymentService.initiatePayment(checkoutSessionId));
    }

    @GetMapping("/status/{paymentId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentStatusResponse> getPaymentStatusById(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.checkPaymentStatus(paymentId));
    }

    @GetMapping("/{paymentId}/status")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentResponse> getPaymentStatus(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.getPaymentStatus(paymentId));
    }

    @PostMapping("/{paymentId}/retry")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentResponse> retryPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.retryPayment(paymentId));
    }

    @PostMapping("/{paymentId}/cancel")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentResponse> cancelPayment(@PathVariable UUID paymentId) {
        return ResponseEntity.ok(paymentService.cancelPayment(paymentId));
    }

    @PostMapping("/webhook")
    public ResponseEntity<PaymentResponse> paymentWebhook(@Valid @RequestBody PaymentWebhookRequest request) {
        return ResponseEntity.ok(paymentService.processWebhook(request));
    }

    @PostMapping("/razorpay/create/{orderId}")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<RazorpayOrderResponse> createRazorpayOrder(@PathVariable UUID orderId) {
        return ResponseEntity.ok(razorpayService.createRazorpayOrder(orderId));
    }

    @PostMapping("/razorpay/verify")
    @PreAuthorize("hasRole('USER') or hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> verifyPayment(@Valid @RequestBody PaymentVerifyRequest request) {
        razorpayService.verifyPayment(
                request.getOrderId(),
                request.getRazorpayOrderId(),
                request.getRazorpayPaymentId(),
                request.getRazorpaySignature()
        );
        return ResponseEntity.ok().build();
    }

    @GetMapping("/admin/all")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<PaymentResponse>> getAllPayments() {
        return ResponseEntity.ok(paymentService.getAllPayments());
    }

    @PostMapping("/admin/{paymentId}/verify")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<PaymentResponse> adminVerifyPayment(
            @PathVariable UUID paymentId,
            @RequestParam boolean approved,
            @RequestParam(required = false) String note) {
        return ResponseEntity.ok(paymentService.verifyPayment(paymentId, approved, note));
    }
}
