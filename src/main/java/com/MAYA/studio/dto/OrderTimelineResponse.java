package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Order;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class OrderTimelineResponse {
    private UUID id;
    private Order.OrderStatus status;
    private String title;
    private String description;
    private String updatedBy;
    private LocalDateTime createdAt;
}
