package com.MAYA.studio.service;

import com.MAYA.studio.dto.AdminDashboardResponse;
import com.MAYA.studio.entity.Order;
import com.MAYA.studio.repository.OrderRepository;
import com.MAYA.studio.repository.ProductRepository;
import com.MAYA.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminDashboardService {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public AdminDashboardResponse getDashboard() {
        List<Order> orders = orderRepository.findAll();
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();

        BigDecimal totalRevenue = orders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Order> todayOrders = orders.stream()
                .filter(o -> o.getCreatedAt() != null && !o.getCreatedAt().isBefore(startOfDay))
                .collect(Collectors.toList());

        BigDecimal todayRevenue = todayOrders.stream()
                .map(Order::getTotalAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long pendingOrders = orders.stream()
                .filter(o -> o.getStatus() == Order.OrderStatus.PENDING_PAYMENT
                        || o.getStatus() == Order.OrderStatus.PAYMENT_RECEIVED
                        || o.getStatus() == Order.OrderStatus.CONFIRMED
                        || o.getStatus() == Order.OrderStatus.PROCESSING)
                .count();

        List<AdminDashboardResponse.RecentOrder> recentOrders = orders.stream()
                .sorted(Comparator.comparing(Order::getCreatedAt).reversed())
                .limit(8)
                .map(o -> AdminDashboardResponse.RecentOrder.builder()
                        .id(o.getId().toString())
                        .orderNumber(o.getOrderNumber())
                        .customerName(o.getShippingFullName())
                        .totalAmount(o.getTotalAmount())
                        .status(o.getStatus().name())
                        .createdAt(o.getCreatedAt().toString())
                        .build())
                .collect(Collectors.toList());

        return AdminDashboardResponse.builder()
                .totalUsers(userRepository.count())
                .totalOrders(orders.size())
                .totalProducts(productRepository.count())
                .pendingOrders(pendingOrders)
                .lowStockProducts(productRepository.countByStockLessThanEqual(5))
                .totalRevenue(totalRevenue)
                .todayRevenue(todayRevenue)
                .todayOrders(todayOrders.size())
                .recentOrders(recentOrders)
                .topProducts(List.of())
                .build();
    }
}
