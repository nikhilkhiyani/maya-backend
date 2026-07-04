package com.MAYA.studio.dto;

import com.MAYA.studio.entity.CheckoutSession;
import com.MAYA.studio.entity.Payment;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class CheckoutSessionResponse {
    private UUID id;
    private List<CheckoutItemResponse> items;
    private String shippingFullName;
    private String shippingPhone;
    private String shippingAddressLine1;
    private String shippingAddressLine2;
    private String shippingCity;
    private String shippingState;
    private String shippingPincode;
    private String billingFullName;
    private String billingPhone;
    private String billingAddressLine1;
    private String billingAddressLine2;
    private String billingCity;
    private String billingState;
    private String billingPincode;
    private boolean billingSameAsShipping;
    private String email;
    private String gstNumber;
    private String couponCode;
    private String orderNotes;
    private BigDecimal subtotal;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private Payment.PaymentMethod paymentMethod;
    private CheckoutSession.SessionStatus status;
    private LocalDateTime expiresAt;
    private LocalDateTime estimatedDelivery;
    private UUID orderId;
    private String orderNumber;

    @Data
    @Builder
    public static class CheckoutItemResponse {
        private UUID productId;
        private String productName;
        private String productSlug;
        private String productImage;
        private Integer quantity;
        private String size;
        private BigDecimal unitPrice;
        private BigDecimal lineTotal;
    }
}
