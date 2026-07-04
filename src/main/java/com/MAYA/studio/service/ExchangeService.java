package com.MAYA.studio.service;

import com.MAYA.studio.dto.ExchangeRequestDto;
import com.MAYA.studio.dto.ExchangeResponse;
import com.MAYA.studio.entity.*;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.ExchangeRequestRepository;
import com.MAYA.studio.repository.OrderRepository;
import com.MAYA.studio.repository.ProductRepository;
import com.MAYA.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeRequestRepository exchangeRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final FileStorageService fileStorageService;
    private final EmailService emailService;
    private final OrderService orderService;
    private final AuditService auditService;

    @Value("${app.exchange.window-days:7}")
    private int exchangeWindowDays;

    private User getCurrentUser() {
        return userRepository.findByEmailOrPhone(SecurityContextHolder.getContext().getAuthentication().getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public ExchangeResponse createExchange(ExchangeRequestDto dto, List<MultipartFile> images, List<MultipartFile> files) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));

        if (!order.getUser().getId().equals(user.getId())) {
            throw new BadRequestException("Access denied");
        }
        if (order.getStatus() != Order.OrderStatus.DELIVERED) {
            throw new BadRequestException("Exchange is only available for delivered orders");
        }
        if (order.getUpdatedAt().plusDays(exchangeWindowDays).isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Exchange window has expired (" + exchangeWindowDays + " days after delivery)");
        }
        if (images == null || images.isEmpty()) {
            throw new BadRequestException("At least one image is required");
        }

        Product product = productRepository.findById(dto.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile image : images) {
            imageUrls.add(fileStorageService.uploadFile(image));
        }
        List<String> fileUrls = new ArrayList<>();
        if (files != null) {
            for (MultipartFile file : files) {
                fileUrls.add(fileStorageService.uploadFile(file));
            }
        }

        ExchangeRequest exchange = ExchangeRequest.builder()
                .order(order)
                .user(user)
                .product(product)
                .reason(dto.getReason())
                .description(dto.getDescription())
                .imageUrls(imageUrls)
                .supportingFileUrls(fileUrls)
                .status(ExchangeRequest.ExchangeStatus.PENDING)
                .build();

        ExchangeRequest saved = exchangeRepository.save(exchange);
        orderService.updateOrderStatus(order.getId(), Order.OrderStatus.EXCHANGE_REQUESTED);

        emailService.sendExchangeAcknowledgment(user, order.getOrderNumber());
        emailService.sendAdminExchangeNotification(order.getOrderNumber());
        auditService.log("EXCHANGE_REQUESTED", "ExchangeRequest", saved.getId().toString(), order.getOrderNumber());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExchangeResponse> getMyExchanges() {
        return exchangeRepository.findByUserIdOrderByCreatedAtDesc(getCurrentUser().getId()).stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<ExchangeResponse> getAllExchanges() {
        return exchangeRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public ExchangeResponse reviewExchange(UUID id, boolean approved, String remarks) {
        ExchangeRequest exchange = exchangeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Exchange not found"));

        if (approved) {
            exchange.setStatus(ExchangeRequest.ExchangeStatus.APPROVED);
            orderService.updateOrderStatus(exchange.getOrder().getId(), Order.OrderStatus.EXCHANGE_APPROVED);
            orderService.updateOrderStatus(exchange.getOrder().getId(), Order.OrderStatus.REFUND_INITIATED);
            emailService.sendExchangeStatusEmail(exchange.getUser(), exchange.getOrder().getOrderNumber(),
                    "Approved", remarks + " Refund will be processed within 2–5 business days.");
        } else {
            exchange.setStatus(ExchangeRequest.ExchangeStatus.REJECTED);
            orderService.updateOrderStatus(exchange.getOrder().getId(), Order.OrderStatus.EXCHANGE_REJECTED);
            emailService.sendExchangeStatusEmail(exchange.getUser(), exchange.getOrder().getOrderNumber(),
                    "Rejected", remarks);
        }

        exchange.setAdminRemarks(remarks);
        ExchangeRequest saved = exchangeRepository.save(exchange);
        auditService.log("EXCHANGE_REVIEWED", "ExchangeRequest", id.toString(), approved ? "APPROVED" : "REJECTED");
        return toResponse(saved);
    }

    private ExchangeResponse toResponse(ExchangeRequest e) {
        return ExchangeResponse.builder()
                .id(e.getId())
                .orderId(e.getOrder().getId())
                .orderNumber(e.getOrder().getOrderNumber())
                .productId(e.getProduct().getId())
                .productName(e.getProduct().getName())
                .reason(e.getReason())
                .description(e.getDescription())
                .imageUrls(e.getImageUrls())
                .supportingFileUrls(e.getSupportingFileUrls())
                .status(e.getStatus())
                .adminRemarks(e.getAdminRemarks())
                .createdAt(e.getCreatedAt())
                .updatedAt(e.getUpdatedAt())
                .build();
    }
}
