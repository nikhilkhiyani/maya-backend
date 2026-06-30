package com.MAYA.studio.repository;

import com.MAYA.studio.entity.ProductReview;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ProductReviewRepository extends JpaRepository<ProductReview, UUID> {
    List<ProductReview> findByProductIdAndStatus(UUID productId, ProductReview.ReviewStatus status);
    List<ProductReview> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ProductReview> findByStatusOrderByCreatedAtDesc(ProductReview.ReviewStatus status);
    Optional<ProductReview> findByOrderIdAndProductIdAndUserId(UUID orderId, UUID productId, UUID userId);
    boolean existsByOrderIdAndProductIdAndUserId(UUID orderId, UUID productId, UUID userId);
}
