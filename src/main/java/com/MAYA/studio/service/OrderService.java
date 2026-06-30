package com.MAYA.studio.service;

import com.MAYA.studio.dto.OrderRequest;
import com.MAYA.studio.dto.OrderResponse;
import com.MAYA.studio.dto.OrderTimelineResponse;
import com.MAYA.studio.dto.ShipmentUpdateRequest;
import com.MAYA.studio.entity.*;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.OrderMapper;
import com.MAYA.studio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartRepository cartRepository;
    private final PaymentRepository paymentRepository;
    private final AddressRepository addressRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final OrderTimelineRepository orderTimelineRepository;
    private final OrderMapper orderMapper;
    private final EmailService emailService;
    private final CouponService couponService;
    private final InvoiceService invoiceService;
    private final AuditService auditService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("999");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("99");
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public OrderResponse createOrder(OrderRequest request) {
        CheckoutRequestAdapter adapter = CheckoutRequestAdapter.fromOrderRequest(request);
        CheckoutSession session = buildSessionFromRequest(adapter, getCurrentUser());
        return createOrderFromSessionEntity(session, request.getPaymentMethod());
    }

    @Transactional
    public Order createOrderFromSession(UUID sessionId) {
        CheckoutSession session = checkoutSessionRepository.findById(sessionId)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));
        if (session.getStatus() == CheckoutSession.SessionStatus.COMPLETED) {
            return session.getOrder();
        }
        OrderResponse response = createOrderFromSessionEntity(session, session.getPaymentMethod());
        return orderRepository.findById(response.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
    }

    private OrderResponse createOrderFromSessionEntity(CheckoutSession session, Payment.PaymentMethod paymentMethod) {
        if (session.getStatus() == CheckoutSession.SessionStatus.COMPLETED && session.getOrder() != null) {
            return toOrderResponse(session.getOrder());
        }

        User user = session.getUser();
        Order.OrderStatus initialStatus = paymentMethod == Payment.PaymentMethod.COD
                ? Order.OrderStatus.CONFIRMED
                : Order.OrderStatus.PAYMENT_RECEIVED;

        Payment.PaymentStatus paymentStatus = paymentMethod == Payment.PaymentMethod.COD
                ? Payment.PaymentStatus.PENDING
                : Payment.PaymentStatus.SUCCESS;

        Order order = Order.builder()
                .user(user)
                .orderNumber(generateOrderNumber())
                .invoiceNumber(generateInvoiceNumber())
                .trackingNumber(generateTrackingNumber())
                .status(initialStatus)
                .paymentMethod(paymentMethod)
                .paymentStatus(paymentStatus)
                .shippingFullName(session.getShippingFullName())
                .shippingPhone(session.getShippingPhone())
                .shippingAddressLine1(session.getShippingAddressLine1())
                .shippingAddressLine2(session.getShippingAddressLine2())
                .shippingCity(session.getShippingCity())
                .shippingState(session.getShippingState())
                .shippingPincode(session.getShippingPincode())
                .billingFullName(session.getBillingFullName())
                .billingPhone(session.getBillingPhone())
                .billingAddressLine1(session.getBillingAddressLine1())
                .billingAddressLine2(session.getBillingAddressLine2())
                .billingCity(session.getBillingCity())
                .billingState(session.getBillingState())
                .billingPincode(session.getBillingPincode())
                .shippingAmount(session.getShippingAmount())
                .taxAmount(session.getTaxAmount())
                .discountAmount(session.getDiscountAmount())
                .totalAmount(session.getTotalAmount())
                .couponCode(session.getCouponCode())
                .orderNotes(session.getOrderNotes())
                .expectedDelivery(session.getEstimatedDelivery())
                .updatedAt(LocalDateTime.now())
                .build();

        List<OrderItem> orderItems = new ArrayList<>();
        for (CheckoutSessionItem sessionItem : session.getItems()) {
            Product product = sessionItem.getProduct();
            if (product.getStock() < sessionItem.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + product.getName());
            }
            orderItems.add(OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(sessionItem.getQuantity())
                    .price(sessionItem.getUnitPrice())
                    .build());
            product.setStock(product.getStock() - sessionItem.getQuantity());
            productRepository.save(product);
        }
        order.setOrderItems(orderItems);

        Order savedOrder = orderRepository.saveAndFlush(order);
        addTimeline(savedOrder, savedOrder.getStatus(), statusTitle(savedOrder.getStatus()), "Order placed", user.getEmail());

        if (paymentMethod == Payment.PaymentMethod.COD) {
            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .checkoutSession(session)
                    .method(Payment.PaymentMethod.COD)
                    .provider(Payment.PaymentProvider.MANUAL)
                    .amount(savedOrder.getTotalAmount())
                    .status(Payment.PaymentStatus.PENDING)
                    .verificationStatus(Payment.VerificationStatus.UNVERIFIED)
                    .build();
            paymentRepository.save(payment);
        }

        cartRepository.deleteByUser(user);

        session.setStatus(CheckoutSession.SessionStatus.COMPLETED);
        session.setOrder(savedOrder);
        checkoutSessionRepository.save(session);

        emailService.sendOrderPlacedEmail(user, savedOrder);
        emailService.sendAdminNewOrderEmail(savedOrder);
        invoiceService.generateAndStoreInvoice(savedOrder);
        auditService.log("ORDER_CREATED", "Order", savedOrder.getId().toString(), savedOrder.getOrderNumber());

        return toOrderResponse(savedOrder);
    }

    private CheckoutSession buildSessionFromRequest(CheckoutRequestAdapter request, User user) {
        resolveShippingAddress(request, user);
        List<CheckoutSessionItem> items = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;

        List<OrderRequest.OrderItemRequest> orderItems = request.getItems();
        if (Boolean.TRUE.equals(request.getFromCart())) {
            orderItems = cartRepository.findByUser(user).stream()
                    .map(c -> new OrderRequest.OrderItemRequest(c.getProduct().getId(), c.getQuantity()))
                    .collect(Collectors.toList());
        }

        for (OrderRequest.OrderItemRequest itemRequest : orderItems) {
            Product product = productRepository.findById(itemRequest.getProductId())
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found"));
            BigDecimal price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            subtotal = subtotal.add(price.multiply(BigDecimal.valueOf(itemRequest.getQuantity())));
            items.add(CheckoutSessionItem.builder().product(product).quantity(itemRequest.getQuantity()).unitPrice(price).build());
        }

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal tax = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = request.getCouponCode() != null && !request.getCouponCode().isBlank()
                ? couponService.calculateDiscount(request.getCouponCode(), subtotal) : BigDecimal.ZERO;

        CheckoutSession session = CheckoutSession.builder()
                .user(user)
                .shippingFullName(request.getShippingFullName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddressLine1(request.getShippingAddressLine1())
                .shippingAddressLine2(request.getShippingAddressLine2())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingPincode(request.getShippingPincode())
                .billingSameAsShipping(true)
                .billingFullName(request.getShippingFullName())
                .billingPhone(request.getShippingPhone())
                .billingAddressLine1(request.getShippingAddressLine1())
                .billingAddressLine2(request.getShippingAddressLine2())
                .billingCity(request.getShippingCity())
                .billingState(request.getShippingState())
                .billingPincode(request.getShippingPincode())
                .couponCode(request.getCouponCode())
                .orderNotes(request.getOrderNotes())
                .termsAccepted(true)
                .subtotal(subtotal)
                .shippingAmount(shipping)
                .taxAmount(tax)
                .discountAmount(discount)
                .totalAmount(subtotal.add(shipping).add(tax).subtract(discount))
                .paymentMethod(request.getPaymentMethod())
                .status(CheckoutSession.SessionStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .estimatedDelivery(LocalDateTime.now().plusDays(5))
                .build();

        for (CheckoutSessionItem item : items) {
            item.setCheckoutSession(session);
            session.getItems().add(item);
        }
        return checkoutSessionRepository.save(session);
    }

    private void resolveShippingAddress(CheckoutRequestAdapter request, User user) {
        if (request.getAddressId() != null) {
            Address address = addressRepository.findByIdAndUser(request.getAddressId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
            request.setShippingFullName(address.getFullName());
            request.setShippingPhone(address.getPhone());
            request.setShippingAddressLine1(address.getAddressLine1());
            request.setShippingAddressLine2(address.getAddressLine2());
            request.setShippingCity(address.getCity());
            request.setShippingState(address.getState());
            request.setShippingPincode(address.getPincode());
        }
        if (request.getShippingFullName() == null || request.getShippingAddressLine1() == null) {
            throw new BadRequestException("Shipping address is required");
        }
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getMyOrders() {
        return orderRepository.findByUserOrderByCreatedAtDesc(getCurrentUser()).stream()
                .map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID id) {
        User user = getCurrentUser();
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (!order.getUser().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Access denied");
        }
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public OrderResponse getOrderByOrderNumber(String orderNumber) {
        User user = getCurrentUser();
        Order order = orderRepository.findByOrderNumberAndUser(orderNumber, user)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        return toOrderResponse(order);
    }

    @Transactional(readOnly = true)
    public List<OrderResponse> getAllOrders() {
        return orderRepository.findAll().stream().map(this::toOrderResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderResponse updateOrderStatus(UUID id, Order.OrderStatus status) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        Order.OrderStatus previous = order.getStatus();

        if (status == Order.OrderStatus.CANCELLED || status == Order.OrderStatus.REFUND_COMPLETED) {
            restoreStock(order);
            order.setPaymentStatus(Payment.PaymentStatus.REFUNDED);
        }

        order.setStatus(status);
        order.setUpdatedAt(LocalDateTime.now());
        Order updated = orderRepository.save(order);

        if (previous != status) {
            addTimeline(updated, status, statusTitle(status), "Status updated", "admin");
            emailService.sendOrderStatusEmail(order.getUser(), updated);
            if (status == Order.OrderStatus.SHIPPED) emailService.sendShipmentConfirmationEmail(order.getUser(), updated);
            if (status == Order.OrderStatus.OUT_FOR_DELIVERY) emailService.sendOutForDeliveryEmail(order.getUser(), updated);
            if (status == Order.OrderStatus.DELIVERED) emailService.sendDeliveredEmail(order.getUser(), updated);
            if (status == Order.OrderStatus.REFUND_INITIATED) emailService.sendRefundInitiatedEmail(order.getUser(), updated);
            if (status == Order.OrderStatus.REFUND_COMPLETED) emailService.sendRefundCompletedEmail(order.getUser(), updated);
        }

        auditService.log("ORDER_STATUS_UPDATED", "Order", id.toString(), previous + " -> " + status);
        return toOrderResponse(updated);
    }

    @Transactional
    public OrderResponse updateShipment(UUID id, ShipmentUpdateRequest request) {
        Order order = orderRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Order not found"));
        if (request.getCourierName() != null) order.setCourierName(request.getCourierName());
        if (request.getTrackingNumber() != null) order.setTrackingNumber(request.getTrackingNumber());
        if (request.getShipmentDate() != null) order.setShipmentDate(request.getShipmentDate());
        if (request.getExpectedDelivery() != null) order.setExpectedDelivery(request.getExpectedDelivery());
        if (request.getShipmentNotes() != null) order.setShipmentNotes(request.getShipmentNotes());
        if (request.getCustomerShipmentNotes() != null) order.setCustomerShipmentNotes(request.getCustomerShipmentNotes());
        if (request.getStatus() != null) order.setStatus(request.getStatus());

        order.setUpdatedAt(LocalDateTime.now());
        Order saved = orderRepository.save(order);

        if (request.getStatus() != null) {
            addTimeline(saved, request.getStatus(), statusTitle(request.getStatus()),
                    request.getCustomerShipmentNotes() != null ? request.getCustomerShipmentNotes() : "Shipment updated", "admin");
            emailService.sendShipmentUpdateEmail(saved.getUser(), saved);
        }

        auditService.log("SHIPMENT_UPDATED", "Order", id.toString(), request.getTrackingNumber());
        return toOrderResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<OrderTimelineResponse> getOrderTimeline(UUID orderId) {
        return orderTimelineRepository.findByOrderIdOrderByCreatedAtAsc(orderId).stream()
                .map(t -> OrderTimelineResponse.builder()
                        .id(t.getId())
                        .status(t.getStatus())
                        .title(t.getTitle())
                        .description(t.getDescription())
                        .updatedBy(t.getUpdatedBy())
                        .createdAt(t.getCreatedAt())
                        .build())
                .collect(Collectors.toList());
    }

    private void addTimeline(Order order, Order.OrderStatus status, String title, String description, String updatedBy) {
        orderTimelineRepository.save(OrderTimeline.builder()
                .order(order)
                .status(status)
                .title(title)
                .description(description)
                .updatedBy(updatedBy)
                .build());
    }

    private String statusTitle(Order.OrderStatus status) {
        return status.name().replace('_', ' ');
    }

    private void restoreStock(Order order) {
        for (OrderItem item : order.getOrderItems()) {
            Product product = item.getProduct();
            product.setStock(product.getStock() + item.getQuantity());
            productRepository.save(product);
        }
    }

    private OrderResponse toOrderResponse(Order order) {
        order.getOrderItems().forEach(item -> item.getProduct().getName());
        return orderMapper.toResponse(order);
    }

    private String generateOrderNumber() {
        return "MAYA" + System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(100, 999);
    }

    private String generateInvoiceNumber() {
        return "INV" + System.currentTimeMillis();
    }

    private String generateTrackingNumber() {
        return "TRK" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }

    @lombok.Data
    private static class CheckoutRequestAdapter {
        private List<OrderRequest.OrderItemRequest> items;
        private UUID addressId;
        private String shippingFullName, shippingPhone, shippingAddressLine1, shippingAddressLine2;
        private String shippingCity, shippingState, shippingPincode;
        private String couponCode, orderNotes;
        private Payment.PaymentMethod paymentMethod;
        private Boolean fromCart;

        static CheckoutRequestAdapter fromOrderRequest(OrderRequest request) {
            CheckoutRequestAdapter a = new CheckoutRequestAdapter();
            a.items = request.getItems();
            a.addressId = request.getAddressId();
            a.shippingFullName = request.getShippingFullName();
            a.shippingPhone = request.getShippingPhone();
            a.shippingAddressLine1 = request.getShippingAddressLine1();
            a.shippingAddressLine2 = request.getShippingAddressLine2();
            a.shippingCity = request.getShippingCity();
            a.shippingState = request.getShippingState();
            a.shippingPincode = request.getShippingPincode();
            a.couponCode = request.getCouponCode();
            a.orderNotes = request.getOrderNotes();
            a.paymentMethod = request.getPaymentMethod();
            a.fromCart = request.getFromCart();
            return a;
        }
    }
}
