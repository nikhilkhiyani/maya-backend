package com.MAYA.studio.dto;

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
public class ProductResponse {
    private UUID id;
    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private List<String> images;
    private Boolean isReadyToShip;
    private BigDecimal rating;
    private LocalDateTime createdAt;
    private String slug;
    private String subcategory;
    private String fabric;
    private Boolean isFeatured;
    private Integer reviews;
}
