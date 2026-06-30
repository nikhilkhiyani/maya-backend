package com.MAYA.studio.dto;

import com.MAYA.studio.entity.ProductReview;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class ReviewResponse {
    private UUID id;
    private UUID productId;
    private String productName;
    private UUID userId;
    private String userName;
    private UUID orderId;
    private Integer rating;
    private String title;
    private String comment;
    private ProductReview.ReviewStatus status;
    private boolean pinned;
    private boolean featured;
    private String adminReply;
    private LocalDateTime createdAt;
}
