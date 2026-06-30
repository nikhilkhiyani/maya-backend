package com.MAYA.studio.dto;

import com.MAYA.studio.entity.ExchangeRequest;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@Builder
public class ExchangeResponse {
    private UUID id;
    private UUID orderId;
    private String orderNumber;
    private UUID productId;
    private String productName;
    private ExchangeRequest.ExchangeReason reason;
    private String description;
    private List<String> imageUrls;
    private List<String> supportingFileUrls;
    private ExchangeRequest.ExchangeStatus status;
    private String adminRemarks;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
