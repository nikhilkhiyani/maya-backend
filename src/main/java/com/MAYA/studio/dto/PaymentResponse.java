package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class PaymentResponse {
    private UUID id;
    private UUID checkoutSessionId;
    private UUID orderId;
    private String orderNumber;
    private Payment.PaymentMethod method;
    private Payment.PaymentStatus status;
    private Payment.VerificationStatus verificationStatus;
    private Payment.PaymentProvider provider;
    private BigDecimal amount;
    private String currency;
    private String transactionId;
    private String failureReason;
    private String upiReference;
    private LocalDateTime expiresAt;
    private LocalDateTime paidAt;
    private LocalDateTime createdAt;
    private String qrImageUrl;
    private List<PaymentLogResponse> logs;

    @Data
    @Builder
    public static class PaymentLogResponse {
        private String event;
        private String details;
        private String actor;
        private LocalDateTime createdAt;
    }
}
