package com.MAYA.studio.controller;

import com.MAYA.studio.dto.AdminDashboardResponse;
import com.MAYA.studio.dto.BannerRequest;
import com.MAYA.studio.dto.BannerResponse;
import com.MAYA.studio.dto.CouponRequest;
import com.MAYA.studio.dto.CouponResponse;
import com.MAYA.studio.service.AdminDashboardService;
import com.MAYA.studio.service.BannerService;
import com.MAYA.studio.service.CouponService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class AdminController {

    private final AdminDashboardService dashboardService;
    private final BannerService bannerService;
    private final CouponService couponService;

    @GetMapping("/admin/dashboard")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<AdminDashboardResponse> getDashboard() {
        return ResponseEntity.ok(dashboardService.getDashboard());
    }

    @GetMapping("/banners/hero")
    public ResponseEntity<List<BannerResponse>> getHeroBanners() {
        return ResponseEntity.ok(bannerService.getActiveHeroBanners());
    }

    @GetMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<BannerResponse>> getAllBanners() {
        return ResponseEntity.ok(bannerService.getAll());
    }

    @PostMapping("/admin/banners")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<BannerResponse> createBanner(@RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.create(request));
    }

    @PutMapping("/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<BannerResponse> updateBanner(@PathVariable UUID id, @RequestBody BannerRequest request) {
        return ResponseEntity.ok(bannerService.update(id, request));
    }

    @DeleteMapping("/admin/banners/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteBanner(@PathVariable UUID id) {
        bannerService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<List<CouponResponse>> getAllCoupons() {
        return ResponseEntity.ok(couponService.getAll());
    }

    @PostMapping("/admin/coupons")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CouponResponse> createCoupon(@RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.create(request));
    }

    @PutMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<CouponResponse> updateCoupon(@PathVariable UUID id, @RequestBody CouponRequest request) {
        return ResponseEntity.ok(couponService.update(id, request));
    }

    @DeleteMapping("/admin/coupons/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @SecurityRequirement(name = "Bearer Authentication")
    public ResponseEntity<Void> deleteCoupon(@PathVariable UUID id) {
        couponService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
