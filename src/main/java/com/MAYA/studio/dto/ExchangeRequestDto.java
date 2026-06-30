package com.MAYA.studio.dto;

import com.MAYA.studio.entity.ExchangeRequest;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.UUID;

@Data
public class ExchangeRequestDto {
    @NotNull
    private UUID orderId;
    @NotNull
    private UUID productId;
    @NotNull
    private ExchangeRequest.ExchangeReason reason;
    @NotBlank
    private String description;
}
