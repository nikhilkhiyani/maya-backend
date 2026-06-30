package com.MAYA.studio.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.util.UUID;

@Data
public class ReviewRequest {
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID productId;
    @NotNull @Min(1) @Max(5)
    private Integer rating;
    @NotBlank
    private String title;
    @NotBlank
    private String comment;
}
