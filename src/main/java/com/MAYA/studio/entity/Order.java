package com.MAYA.studio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderItem> orderItems = new ArrayList<>();

    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<OrderTimeline> timeline = new ArrayList<>();

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Column(unique = true)
    private String orderNumber;

    @Column(unique = true)
    private String invoiceNumber;

    @Column(unique = true)
    private String trackingNumber;

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

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal shippingAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal taxAmount = BigDecimal.ZERO;

    @Column(precision = 10, scale = 2)
    @Builder.Default
    private BigDecimal discountAmount = BigDecimal.ZERO;

    private String couponCode;
    private String orderNotes;
    private String courierName;
    private String shipmentNotes;
    private String customerShipmentNotes;
    private LocalDateTime shipmentDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Payment.PaymentMethod paymentMethod = Payment.PaymentMethod.COD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Payment.PaymentStatus paymentStatus = Payment.PaymentStatus.PENDING;

    private LocalDateTime expectedDelivery;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private OrderStatus status = OrderStatus.PENDING_PAYMENT;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    public enum OrderStatus {
        PENDING_PAYMENT,
        PAYMENT_RECEIVED,
        CONFIRMED,
        PROCESSING,
        PACKED,
        READY_TO_SHIP,
        SHIPPED,
        OUT_FOR_DELIVERY,
        DELIVERED,
        EXCHANGE_REQUESTED,
        EXCHANGE_UNDER_REVIEW,
        EXCHANGE_APPROVED,
        EXCHANGE_REJECTED,
        REFUND_INITIATED,
        REFUND_COMPLETED,
        CANCELLED
    }
}
