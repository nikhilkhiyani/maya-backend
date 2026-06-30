package com.MAYA.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductRequest {

    private String name;
    private String description;
    private String category;
    private BigDecimal price;
    private BigDecimal discountPrice;
    private Integer stock;
    private List<String> images;
    private Boolean isReadyToShip;
    private String slug;
    private String subcategory;
    private String fabric;
    private Boolean isFeatured;
    private BigDecimal rating;
    private Integer reviews;
}
