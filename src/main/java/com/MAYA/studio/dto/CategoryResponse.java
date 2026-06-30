package com.MAYA.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {
    private UUID id;
    private String name;
    private String slug;
    private String code;
    private String description;
    private String image;
    private String bannerImage;
    private Integer displayOrder;
    private Boolean enabled;
    private Boolean featured;
    private Boolean showOnHomepage;
    private String seoTitle;
    private String seoDescription;
    private LocalDateTime createdAt;
    private Long productCount;
}
