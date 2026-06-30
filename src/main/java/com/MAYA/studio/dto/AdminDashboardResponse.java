package com.MAYA.studio.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminDashboardResponse {
    private long totalUsers;
    private long totalOrders;
    private long totalProducts;
    private long pendingOrders;
    private long lowStockProducts;
    private BigDecimal totalRevenue;
    private BigDecimal todayRevenue;
    private long todayOrders;
    private List<TopProduct> topProducts;
    private List<RecentOrder> recentOrders;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TopProduct {
        private String id;
        private String name;
        private long soldCount;
        private BigDecimal revenue;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentOrder {
        private String id;
        private String orderNumber;
        private String customerName;
        private BigDecimal totalAmount;
        private String status;
        private String createdAt;
    }
}
