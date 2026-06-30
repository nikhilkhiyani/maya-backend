package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class PaymentStatusResponse {
    private UUID paymentId;
    private Payment.PaymentStatus status;
    private UUID orderId;
    private String orderNumber;
    private String failureReason;
}
