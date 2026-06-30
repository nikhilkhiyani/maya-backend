package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.Payment;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderResponse {
    private UUID id;
    private UUID userId;
    private String orderNumber;
    private String invoiceNumber;
    private String trackingNumber;
    private List<OrderItemResponse> orderItems;
    private BigDecimal totalAmount;
    private BigDecimal shippingAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private String couponCode;
    private String orderNotes;
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
    private String courierName;
    private String shipmentNotes;
    private String customerShipmentNotes;
    private LocalDateTime shipmentDate;
    private Payment.PaymentMethod paymentMethod;
    private Payment.PaymentStatus paymentStatus;
    private Order.OrderStatus status;
    private LocalDateTime expectedDelivery;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class OrderItemResponse {
        private UUID id;
        private ProductResponse product;
        private Integer quantity;
        private BigDecimal price;
    }
}
