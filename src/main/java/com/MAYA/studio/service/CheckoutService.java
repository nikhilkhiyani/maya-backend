package com.MAYA.studio.service;

import com.MAYA.studio.dto.CheckoutRequest;
import com.MAYA.studio.dto.CheckoutSessionResponse;
import com.MAYA.studio.entity.*;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CheckoutSessionRepository checkoutSessionRepository;
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final CouponService couponService;
    private final OrderService orderService;
    private final AuditService auditService;

    private static final BigDecimal FREE_SHIPPING_THRESHOLD = new BigDecimal("999");
    private static final BigDecimal SHIPPING_COST = new BigDecimal("99");
    private static final BigDecimal GST_RATE = new BigDecimal("0.18");

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmailOrPhone(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public CheckoutSessionResponse createSession(CheckoutRequest request) {
        if (!request.isTermsAccepted()) {
            throw new BadRequestException("You must accept the terms and conditions");
        }

        User user = getCurrentUser();
        resolveAddresses(request, user);

        List<CheckoutSessionItem> items = buildItemsFromCart(user);
        if (items.isEmpty()) {
            throw new BadRequestException("Cart is empty");
        }

        BigDecimal subtotal = items.stream()
                .map(i -> i.getUnitPrice().multiply(BigDecimal.valueOf(i.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal shipping = subtotal.compareTo(FREE_SHIPPING_THRESHOLD) >= 0 ? BigDecimal.ZERO : SHIPPING_COST;
        BigDecimal tax = subtotal.multiply(GST_RATE).setScale(2, RoundingMode.HALF_UP);
        BigDecimal discount = BigDecimal.ZERO;
        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            discount = couponService.calculateDiscount(request.getCouponCode(), subtotal);
        }
        BigDecimal total = subtotal.add(shipping).add(tax).subtract(discount);

        CheckoutSession session = CheckoutSession.builder()
                .user(user)
                .shippingFullName(request.getShippingFullName())
                .shippingPhone(request.getShippingPhone())
                .shippingAddressLine1(request.getShippingAddressLine1())
                .shippingAddressLine2(request.getShippingAddressLine2())
                .shippingCity(request.getShippingCity())
                .shippingState(request.getShippingState())
                .shippingPincode(request.getShippingPincode())
                .billingSameAsShipping(request.isBillingSameAsShipping())
                .billingFullName(request.isBillingSameAsShipping() ? request.getShippingFullName() : request.getBillingFullName())
                .billingPhone(request.isBillingSameAsShipping() ? request.getShippingPhone() : request.getBillingPhone())
                .billingAddressLine1(request.isBillingSameAsShipping() ? request.getShippingAddressLine1() : request.getBillingAddressLine1())
                .billingAddressLine2(request.isBillingSameAsShipping() ? request.getShippingAddressLine2() : request.getBillingAddressLine2())
                .billingCity(request.isBillingSameAsShipping() ? request.getShippingCity() : request.getBillingCity())
                .billingState(request.isBillingSameAsShipping() ? request.getShippingState() : request.getBillingState())
                .billingPincode(request.isBillingSameAsShipping() ? request.getShippingPincode() : request.getBillingPincode())
                .email(request.getEmail() != null ? request.getEmail() : user.getEmail())
                .gstNumber(request.getGstNumber())
                .couponCode(request.getCouponCode())
                .orderNotes(request.getOrderNotes())
                .termsAccepted(true)
                .subtotal(subtotal)
                .shippingAmount(shipping)
                .taxAmount(tax)
                .discountAmount(discount)
                .totalAmount(total)
                .paymentMethod(request.getPaymentMethod())
                .status(CheckoutSession.SessionStatus.ACTIVE)
                .expiresAt(LocalDateTime.now().plusMinutes(30))
                .estimatedDelivery(LocalDateTime.now().plusDays(5))
                .build();

        for (CheckoutSessionItem item : items) {
            item.setCheckoutSession(session);
            session.getItems().add(item);
        }

        CheckoutSession saved = checkoutSessionRepository.save(session);
        auditService.log("CHECKOUT_SESSION_CREATED", "CheckoutSession", saved.getId().toString(),
                "Total: " + total + ", Method: " + request.getPaymentMethod());

        if (request.getPaymentMethod() == Payment.PaymentMethod.COD) {
            orderService.createOrderFromSession(saved.getId());
            saved = checkoutSessionRepository.findById(saved.getId()).orElse(saved);
        }

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public CheckoutSessionResponse getSession(UUID sessionId) {
        User user = getCurrentUser();
        CheckoutSession session = checkoutSessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));
        return toResponse(session);
    }

    private List<CheckoutSessionItem> buildItemsFromCart(User user) {
        List<CheckoutSessionItem> items = new ArrayList<>();
        for (Cart cart : cartRepository.findByUser(user)) {
            Product product = cart.getProduct();
            if (product.getStock() < cart.getQuantity()) {
                throw new BadRequestException("Insufficient stock for: " + product.getName());
            }
            BigDecimal price = product.getDiscountPrice() != null ? product.getDiscountPrice() : product.getPrice();
            items.add(CheckoutSessionItem.builder()
                    .product(product)
                    .quantity(cart.getQuantity())
                    .size(cart.getSize())
                    .unitPrice(price)
                    .build());
        }
        return items;
    }

    private void resolveAddresses(CheckoutRequest request, User user) {
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

        if (!request.isBillingSameAsShipping() && request.getBillingAddressId() != null) {
            Address billing = addressRepository.findByIdAndUser(request.getBillingAddressId(), user)
                    .orElseThrow(() -> new ResourceNotFoundException("Billing address not found"));
            request.setBillingFullName(billing.getFullName());
            request.setBillingPhone(billing.getPhone());
            request.setBillingAddressLine1(billing.getAddressLine1());
            request.setBillingAddressLine2(billing.getAddressLine2());
            request.setBillingCity(billing.getCity());
            request.setBillingState(billing.getState());
            request.setBillingPincode(billing.getPincode());
        }

        if (request.getShippingFullName() == null || request.getShippingAddressLine1() == null) {
            throw new BadRequestException("Shipping address is required");
        }
    }

    public CheckoutSessionResponse toResponse(CheckoutSession session) {
        return CheckoutSessionResponse.builder()
                .id(session.getId())
                .items(session.getItems().stream().map(item -> CheckoutSessionResponse.CheckoutItemResponse.builder()
                        .productId(item.getProduct().getId())
                        .productName(item.getProduct().getName())
                        .productSlug(item.getProduct().getSlug())
                        .productImage(item.getProduct().getImages().isEmpty() ? null : item.getProduct().getImages().get(0))
                        .quantity(item.getQuantity())
                        .size(item.getSize())
                        .unitPrice(item.getUnitPrice())
                        .lineTotal(item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                        .build()).collect(Collectors.toList()))
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
                .billingSameAsShipping(session.isBillingSameAsShipping())
                .email(session.getEmail())
                .gstNumber(session.getGstNumber())
                .couponCode(session.getCouponCode())
                .orderNotes(session.getOrderNotes())
                .subtotal(session.getSubtotal())
                .shippingAmount(session.getShippingAmount())
                .taxAmount(session.getTaxAmount())
                .discountAmount(session.getDiscountAmount())
                .totalAmount(session.getTotalAmount())
                .paymentMethod(session.getPaymentMethod())
                .status(session.getStatus())
                .expiresAt(session.getExpiresAt())
                .estimatedDelivery(session.getEstimatedDelivery())
                .orderId(session.getOrder() != null ? session.getOrder().getId() : null)
                .orderNumber(session.getOrder() != null ? session.getOrder().getOrderNumber() : null)
                .build();
    }
}
