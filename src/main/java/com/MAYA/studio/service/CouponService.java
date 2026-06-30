package com.MAYA.studio.service;

import com.MAYA.studio.dto.CouponRequest;
import com.MAYA.studio.dto.CouponResponse;
import com.MAYA.studio.entity.Coupon;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.CouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;

    public List<CouponResponse> getAll() {
        return couponRepository.findAll().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CouponResponse create(CouponRequest request) {
        if (couponRepository.findByCodeIgnoreCase(request.getCode()).isPresent()) {
            throw new BadRequestException("Coupon code already exists");
        }

        Coupon coupon = Coupon.builder()
                .code(request.getCode().toUpperCase())
                .description(request.getDescription())
                .type(request.getType())
                .value(request.getValue())
                .minCartAmount(request.getMinCartAmount())
                .maxDiscount(request.getMaxDiscount())
                .usageLimit(request.getUsageLimit())
                .validFrom(request.getValidFrom())
                .validUntil(request.getValidUntil())
                .active(request.getActive() != null ? request.getActive() : true)
                .build();

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public CouponResponse update(UUID id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Coupon not found"));

        if (request.getDescription() != null) coupon.setDescription(request.getDescription());
        if (request.getType() != null) coupon.setType(request.getType());
        if (request.getValue() != null) coupon.setValue(request.getValue());
        if (request.getMinCartAmount() != null) coupon.setMinCartAmount(request.getMinCartAmount());
        if (request.getMaxDiscount() != null) coupon.setMaxDiscount(request.getMaxDiscount());
        if (request.getUsageLimit() != null) coupon.setUsageLimit(request.getUsageLimit());
        if (request.getValidFrom() != null) coupon.setValidFrom(request.getValidFrom());
        if (request.getValidUntil() != null) coupon.setValidUntil(request.getValidUntil());
        if (request.getActive() != null) coupon.setActive(request.getActive());

        return toResponse(couponRepository.save(coupon));
    }

    @Transactional
    public void delete(UUID id) {
        if (!couponRepository.existsById(id)) {
            throw new ResourceNotFoundException("Coupon not found");
        }
        couponRepository.deleteById(id);
    }

    @Transactional
    public BigDecimal calculateDiscount(String code, BigDecimal subtotal) {
        if (code == null || code.isBlank()) {
            return BigDecimal.ZERO;
        }

        Coupon coupon = couponRepository.findByCodeIgnoreCase(code.trim())
                .orElseThrow(() -> new BadRequestException("Invalid coupon code"));

        validateCoupon(coupon, subtotal);

        BigDecimal discount = switch (coupon.getType()) {
            case PERCENTAGE -> subtotal.multiply(coupon.getValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            case FLAT -> coupon.getValue();
            case FREE_SHIPPING -> BigDecimal.ZERO;
        };

        if (coupon.getMaxDiscount() != null && discount.compareTo(coupon.getMaxDiscount()) > 0) {
            discount = coupon.getMaxDiscount();
        }

        coupon.setUsedCount(coupon.getUsedCount() + 1);
        couponRepository.save(coupon);

        return discount.max(BigDecimal.ZERO);
    }

    private void validateCoupon(Coupon coupon, BigDecimal subtotal) {
        if (!Boolean.TRUE.equals(coupon.getActive())) {
            throw new BadRequestException("Coupon is not active");
        }
        LocalDateTime now = LocalDateTime.now();
        if (coupon.getValidFrom() != null && now.isBefore(coupon.getValidFrom())) {
            throw new BadRequestException("Coupon is not yet valid");
        }
        if (coupon.getValidUntil() != null && now.isAfter(coupon.getValidUntil())) {
            throw new BadRequestException("Coupon has expired");
        }
        if (coupon.getUsageLimit() != null && coupon.getUsedCount() >= coupon.getUsageLimit()) {
            throw new BadRequestException("Coupon usage limit reached");
        }
        if (coupon.getMinCartAmount() != null && subtotal.compareTo(coupon.getMinCartAmount()) < 0) {
            throw new BadRequestException("Minimum cart amount not met for this coupon");
        }
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .type(coupon.getType())
                .value(coupon.getValue())
                .minCartAmount(coupon.getMinCartAmount())
                .maxDiscount(coupon.getMaxDiscount())
                .usageLimit(coupon.getUsageLimit())
                .usedCount(coupon.getUsedCount())
                .validFrom(coupon.getValidFrom())
                .validUntil(coupon.getValidUntil())
                .active(coupon.getActive())
                .createdAt(coupon.getCreatedAt())
                .build();
    }
}
