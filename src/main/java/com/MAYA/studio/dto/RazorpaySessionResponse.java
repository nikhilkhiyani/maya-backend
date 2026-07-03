package com.MAYA.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Everything the Razorpay Checkout widget needs to open for a checkout session.
 * The domain Order is created only after the payment signature is verified.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RazorpaySessionResponse {
    private UUID paymentId;
    private UUID checkoutSessionId;
    private String razorpayOrderId;
    private String razorpayKeyId;
    private BigDecimal amount;
    private int amountInPaise;
    private String currency;
    private String customerName;
    private String customerEmail;
    private String customerContact;
}
