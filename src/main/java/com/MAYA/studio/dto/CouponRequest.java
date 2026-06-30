package com.MAYA.studio.dto;

import com.MAYA.studio.entity.Coupon;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponRequest {
    private String code;
    private String description;
    private Coupon.CouponType type;
    private BigDecimal value;
    private BigDecimal minCartAmount;
    private BigDecimal maxDiscount;
    private Integer usageLimit;
    private LocalDateTime validFrom;
    private LocalDateTime validUntil;
    private Boolean active;
}
