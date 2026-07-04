package com.MAYA.studio.service;

import com.MAYA.studio.dto.ReviewRequest;
import com.MAYA.studio.dto.ReviewResponse;
import com.MAYA.studio.entity.*;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.OrderRepository;
import com.MAYA.studio.repository.ProductRepository;
import com.MAYA.studio.repository.ProductReviewRepository;
import com.MAYA.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    private User getCurrentUser() {
        return userRepository.findByEmailOrPhone(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("You can only review products from delivered orders");
        }
        if (reviewRepository.existsByOrderIdAndProductIdAndUserId(order.getId(), request.getProductId(), user.getId())) {
            throw new BadRequestException("You have already reviewed this product for this order");
        }

        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        ProductReview review = ProductReview.builder()
                .product(product)
                .user(user)
                .order(order)
                .rating(request.getRating())
                .title(request.getTitle())
                .comment(request.getComment())
                .status(ProductReview.ReviewStatus.PENDING)
                .build();

        ProductReview saved = reviewRepository.save(review);
        auditService.log("REVIEW_SUBMITTED", "ProductReview", saved.getId().toString(), product.getName());
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getProductReviews(UUID productId) {
        return reviewRepository.findByProductIdAndStatus(productId, ProductReview.ReviewStatus.APPROVED).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getMyReviews() {
        return reviewRepository.findByUserIdOrderByCreatedAtDesc(getCurrentUser().getId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ReviewResponse> getPendingReviews() {
        return reviewRepository.findByStatusOrderByCreatedAtDesc(ProductReview.ReviewStatus.PENDING).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ReviewResponse moderateReview(UUID id, ProductReview.ReviewStatus status, String adminReply) {
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setStatus(status);
        if (adminReply != null) {
            review.setAdminReply(adminReply);
            review.setRepliedAt(LocalDateTime.now());
        }
        ProductReview saved = reviewRepository.save(review);
        if (status == ProductReview.ReviewStatus.APPROVED) {
            updateProductRating(saved.getProduct());
        }
        auditService.log("REVIEW_MODERATED", "ProductReview", id.toString(), status.name());
        return toResponse(saved);
    }

    @Transactional
    public void deleteReview(UUID id) {
        reviewRepository.deleteById(id);
        auditService.log("REVIEW_DELETED", "ProductReview", id.toString(), null);
    }

    @Transactional
    public ReviewResponse pinReview(UUID id, boolean pinned) {
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setPinned(pinned);
        return toResponse(reviewRepository.save(review));
    }

    @Transactional
    public ReviewResponse featureReview(UUID id, boolean featured) {
        ProductReview review = reviewRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        review.setFeatured(featured);
        return toResponse(reviewRepository.save(review));
    }

    public boolean canReview(UUID orderId, UUID productId) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null || order.getStatus() != Order.OrderStatus.DELIVERED) return false;
        if (!order.getUser().getId().equals(user.getId())) return false;
        return !reviewRepository.existsByOrderIdAndProductIdAndUserId(orderId, productId, user.getId());
    }

    private void updateProductRating(Product product) {
        List<ProductReview> approved = reviewRepository.findByProductIdAndStatus(product.getId(), ProductReview.ReviewStatus.APPROVED);
        if (approved.isEmpty()) return;
        double avg = approved.stream().mapToInt(ProductReview::getRating).average().orElse(0);
        product.setRating(java.math.BigDecimal.valueOf(avg));
        product.setReviews(approved.size());
        productRepository.save(product);
    }

    private ReviewResponse toResponse(ProductReview r) {
        return ReviewResponse.builder()
                .id(r.getId())
                .productId(r.getProduct().getId())
                .productName(r.getProduct().getName())
                .userId(r.getUser().getId())
                .userName(r.getUser().getName())
                .orderId(r.getOrder().getId())
                .rating(r.getRating())
                .title(r.getTitle())
                .comment(r.getComment())
                .status(r.getStatus())
                .pinned(r.isPinned())
                .featured(r.isFeatured())
                .adminReply(r.getAdminReply())
                .createdAt(r.getCreatedAt())
                .build();
    }
}
