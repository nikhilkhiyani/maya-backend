package com.MAYA.studio.dto;

import lombok.Data;

import java.util.UUID;

@Data
public class PaymentWebhookRequest {
    private UUID paymentId;
    private String status;
    private String transactionId;
    private String failureReason;
    private String signature;
}
