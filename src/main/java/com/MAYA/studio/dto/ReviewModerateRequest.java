package com.MAYA.studio.dto;

import com.MAYA.studio.entity.ProductReview;
import lombok.Data;

@Data
public class ReviewModerateRequest {
    private ProductReview.ReviewStatus status;
    private String adminReply;
}
