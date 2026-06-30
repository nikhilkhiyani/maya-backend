package com.MAYA.studio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "exchange_requests", indexes = {
        @Index(name = "idx_exchange_order", columnList = "order_id"),
        @Index(name = "idx_exchange_user", columnList = "user_id"),
        @Index(name = "idx_exchange_status", columnList = "status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExchangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ExchangeReason reason;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @ElementCollection
    @CollectionTable(name = "exchange_images", joinColumns = @JoinColumn(name = "exchange_id"))
    @Column(name = "image_url")
    @Builder.Default
    private List<String> imageUrls = new ArrayList<>();

    @ElementCollection
    @CollectionTable(name = "exchange_files", joinColumns = @JoinColumn(name = "exchange_id"))
    @Column(name = "file_url")
    @Builder.Default
    private List<String> supportingFileUrls = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private ExchangeStatus status = ExchangeStatus.PENDING;

    @Column(columnDefinition = "TEXT")
    private String adminRemarks;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    public enum ExchangeReason {
        DEFECTIVE, DAMAGED, INCORRECT_PRODUCT
    }

    public enum ExchangeStatus {
        PENDING, UNDER_REVIEW, APPROVED, REJECTED, REFUND_INITIATED, REFUND_COMPLETED
    }
}
