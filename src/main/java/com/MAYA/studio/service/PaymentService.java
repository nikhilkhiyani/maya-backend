package com.MAYA.studio.service;

import com.MAYA.studio.dto.PaymentResponse;
import com.MAYA.studio.dto.PaymentStatusResponse;
import com.MAYA.studio.dto.PaymentWebhookRequest;
import com.MAYA.studio.dto.RazorpaySessionResponse;
import com.MAYA.studio.entity.*;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final PaymentLogRepository paymentLogRepository;
    private final CheckoutSessionRepository checkoutSessionRepository;
    private final UserRepository userRepository;
    private final OrderService orderService;
    private final EmailService emailService;
    private final AuditService auditService;
    private final RazorpayService razorpayService;

    @Value("${app.payment.qr-image-url:/uploads/payment-qr.png}")
    private String qrImageUrl;

    @Value("${app.payment.timeout-minutes:15}")
    private int paymentTimeoutMinutes;

    @Value("${app.payment.webhook-secret:}")
    private String webhookSecret;

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    @Transactional
    public PaymentResponse initiatePayment(UUID checkoutSessionId) {
        User user = getCurrentUser();
        CheckoutSession session = checkoutSessionRepository.findByIdAndUser(checkoutSessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));

        if (session.getStatus() == CheckoutSession.SessionStatus.COMPLETED) {
            Payment existing = paymentRepository.findTopByCheckoutSessionIdOrderByCreatedAtDesc(checkoutSessionId)
                    .orElse(null);
            if (existing != null && existing.getStatus() == Payment.PaymentStatus.SUCCESS) {
                return toResponse(existing);
            }
            throw new BadRequestException("Checkout session already completed");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(CheckoutSession.SessionStatus.EXPIRED);
            checkoutSessionRepository.save(session);
            throw new BadRequestException("Checkout session has expired");
        }
        if (session.getPaymentMethod() == Payment.PaymentMethod.COD) {
            throw new BadRequestException("COD orders do not require online payment");
        }

        Payment payment = paymentRepository.findTopByCheckoutSessionIdOrderByCreatedAtDesc(checkoutSessionId)
                .orElse(null);

        if (payment != null) {
            expireIfNeeded(payment);
            if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
                return toResponse(payment);
            }
            if (payment.getStatus() == Payment.PaymentStatus.PENDING && !isExpired(payment)) {
                session.setStatus(CheckoutSession.SessionStatus.PAYMENT_PENDING);
                checkoutSessionRepository.save(session);
                return toResponse(payment);
            }
        }

        payment = Payment.builder()
                .checkoutSession(session)
                .method(session.getPaymentMethod())
                .provider(mapProvider(session.getPaymentMethod()))
                .amount(session.getTotalAmount())
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .verificationStatus(Payment.VerificationStatus.UNVERIFIED)
                .expiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes))
                .build();
        payment = paymentRepository.save(payment);
        addLog(payment, "PAYMENT_INITIATED", "Payment initiated for checkout session", user.getEmail());

        session.setStatus(CheckoutSession.SessionStatus.PAYMENT_PENDING);
        checkoutSessionRepository.save(session);

        auditService.log("PAYMENT_INITIATED", "Payment", payment.getId().toString(),
                "Amount: " + payment.getAmount());

        return toResponse(payment);
    }

    /**
     * Creates a Razorpay order for a checkout session and a matching PENDING payment row.
     * The domain Order is intentionally NOT created here — only after the signature is verified.
     */
    @Transactional
    public RazorpaySessionResponse createRazorpaySessionOrder(UUID checkoutSessionId) {
        User user = getCurrentUser();
        CheckoutSession session = checkoutSessionRepository.findByIdAndUser(checkoutSessionId, user)
                .orElseThrow(() -> new ResourceNotFoundException("Checkout session not found"));

        if (session.getStatus() == CheckoutSession.SessionStatus.COMPLETED) {
            throw new BadRequestException("Checkout session already completed");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            session.setStatus(CheckoutSession.SessionStatus.EXPIRED);
            checkoutSessionRepository.save(session);
            throw new BadRequestException("Checkout session has expired");
        }
        if (session.getPaymentMethod() != Payment.PaymentMethod.RAZORPAY) {
            throw new BadRequestException("This checkout session is not a Razorpay payment");
        }

        Payment payment = paymentRepository.findTopByCheckoutSessionIdOrderByCreatedAtDesc(checkoutSessionId)
                .orElse(null);

        // Reuse an existing pending Razorpay order for this session; otherwise create a fresh one.
        if (payment == null
                || payment.getStatus() != Payment.PaymentStatus.PENDING
                || payment.getRazorpayOrderId() == null
                || isExpired(payment)) {
            String razorpayOrderId = razorpayService.createApiOrder(
                    session.getTotalAmount(), "s_" + session.getId());

            payment = Payment.builder()
                    .checkoutSession(session)
                    .method(Payment.PaymentMethod.RAZORPAY)
                    .provider(Payment.PaymentProvider.RAZORPAY)
                    .amount(session.getTotalAmount())
                    .currency("INR")
                    .status(Payment.PaymentStatus.PENDING)
                    .verificationStatus(Payment.VerificationStatus.UNVERIFIED)
                    .razorpayOrderId(razorpayOrderId)
                    .expiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes))
                    .build();
            payment = paymentRepository.save(payment);
            addLog(payment, "PAYMENT_INITIATED", "Razorpay order created: " + razorpayOrderId, user.getEmail());
        }

        session.setStatus(CheckoutSession.SessionStatus.PAYMENT_PENDING);
        checkoutSessionRepository.save(session);

        return RazorpaySessionResponse.builder()
                .paymentId(payment.getId())
                .checkoutSessionId(session.getId())
                .razorpayOrderId(payment.getRazorpayOrderId())
                .razorpayKeyId(razorpayService.getKeyId())
                .amount(payment.getAmount())
                .amountInPaise(razorpayService.toPaise(payment.getAmount()))
                .currency(payment.getCurrency())
                .customerName(session.getShippingFullName())
                .customerEmail(session.getEmail() != null ? session.getEmail() : user.getEmail())
                .customerContact(session.getShippingPhone())
                .build();
    }

    /**
     * Verifies the Razorpay signature for a session payment and, on success, completes it
     * (creates the order, clears the cart, sends email) via the shared success path.
     */
    @Transactional
    public PaymentResponse verifyRazorpaySessionPayment(UUID paymentId, String razorpayOrderId,
                                                        String razorpayPaymentId, String razorpaySignature) {
        Payment payment = getPaymentForUser(paymentId);

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        if (payment.getRazorpayOrderId() == null || !payment.getRazorpayOrderId().equals(razorpayOrderId)) {
            throw new BadRequestException("Razorpay order mismatch for this payment");
        }
        if (!razorpayService.verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            markPaymentFailed(payment, "Invalid Razorpay signature", "razorpay");
            throw new BadRequestException("Payment verification failed");
        }

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setUpiReference(razorpayPaymentId);
        paymentRepository.save(payment);

        return completePaymentSuccess(payment, razorpayPaymentId, "razorpay");
    }

    /**
     * Server-to-server Razorpay webhook. This is the source of truth for order creation:
     * even if the customer closes the browser after paying, this guarantees the order is
     * created. Idempotent — replays and duplicate events are safely ignored.
     */
    @Transactional
    public void handleRazorpayWebhook(String rawBody, String signature) {
        if (!razorpayService.verifyWebhookSignature(rawBody, signature)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        tools.jackson.databind.JsonNode root;
        try {
            root = new tools.jackson.databind.ObjectMapper().readTree(rawBody);
        } catch (Exception e) {
            throw new BadRequestException("Malformed webhook payload");
        }

        String event = root.path("event").asText("");
        tools.jackson.databind.JsonNode paymentEntity = root.path("payload").path("payment").path("entity");
        String razorpayOrderId = textOrNull(paymentEntity.path("order_id"));
        String razorpayPaymentId = textOrNull(paymentEntity.path("id"));

        if (razorpayOrderId == null) {
            return;
        }

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            return;
        }

        switch (event) {
            case "payment.captured", "order.paid" -> {
                if (payment.getStatus() != Payment.PaymentStatus.SUCCESS
                        && payment.getCheckoutSession() != null) {
                    payment.setRazorpayPaymentId(razorpayPaymentId);
                    payment.setUpiReference(razorpayPaymentId);
                    paymentRepository.save(payment);
                    completePaymentSuccess(payment, razorpayPaymentId, "razorpay-webhook");
                }
            }
            case "payment.failed" -> {
                if (payment.getStatus() == Payment.PaymentStatus.PENDING) {
                    String reason = textOrNull(paymentEntity.path("error_description"));
                    markPaymentFailed(payment, reason != null ? reason : "Payment failed at gateway", "razorpay-webhook");
                }
            }
            default -> {
                // Other events (refunds, etc.) are not handled here.
            }
        }
    }

    private String textOrNull(tools.jackson.databind.JsonNode node) {
        return node == null || node.isMissingNode() || node.isNull() ? null : node.asText();
    }

    @Transactional
    public PaymentStatusResponse checkPaymentStatus(UUID paymentId) {
        Payment payment = getPaymentForUser(paymentId);
        expireIfNeeded(payment);
        return toStatusResponse(payment);
    }

    @Transactional
    public PaymentResponse getPaymentStatus(UUID paymentId) {
        Payment payment = getPaymentForUser(paymentId);
        expireIfNeeded(payment);
        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse retryPayment(UUID paymentId) {
        User user = getCurrentUser();
        Payment oldPayment = getPaymentForUser(paymentId);
        CheckoutSession session = oldPayment.getCheckoutSession();

        if (session.getStatus() == CheckoutSession.SessionStatus.COMPLETED) {
            throw new BadRequestException("Order already created for this session");
        }
        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Checkout session has expired. Please start checkout again.");
        }

        Payment payment = Payment.builder()
                .checkoutSession(session)
                .method(session.getPaymentMethod())
                .provider(mapProvider(session.getPaymentMethod()))
                .amount(session.getTotalAmount())
                .currency("INR")
                .status(Payment.PaymentStatus.PENDING)
                .verificationStatus(Payment.VerificationStatus.UNVERIFIED)
                .expiresAt(LocalDateTime.now().plusMinutes(paymentTimeoutMinutes))
                .build();
        payment = paymentRepository.save(payment);
        addLog(payment, "PAYMENT_RETRY", "New payment session created", user.getEmail());

        session.setStatus(CheckoutSession.SessionStatus.PAYMENT_PENDING);
        checkoutSessionRepository.save(session);

        auditService.log("PAYMENT_RETRY", "Payment", payment.getId().toString(),
                "Previous: " + oldPayment.getId());

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse cancelPayment(UUID paymentId) {
        User user = getCurrentUser();
        Payment payment = getPaymentForUser(paymentId);

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            throw new BadRequestException("Cannot cancel a completed payment");
        }
        if (payment.getStatus() == Payment.PaymentStatus.CANCELLED) {
            return toResponse(payment);
        }

        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setFailureReason("Payment cancelled by user");
        paymentRepository.save(payment);
        addLog(payment, "PAYMENT_CANCELLED", "Cancelled by user", user.getEmail());

        return toResponse(payment);
    }

    @Transactional
    public PaymentResponse processWebhook(PaymentWebhookRequest request) {
        if (request.getPaymentId() == null) {
            throw new BadRequestException("paymentId is required");
        }
        if (!webhookSecret.isBlank() && !verifyWebhookSignature(request)) {
            throw new BadRequestException("Invalid webhook signature");
        }

        Payment payment = paymentRepository.findById(request.getPaymentId())
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        String status = request.getStatus() != null ? request.getStatus().toUpperCase() : "";

        return switch (status) {
            case "SUCCESS", "COMPLETED", "CAPTURED" -> completePaymentSuccess(
                    payment, request.getTransactionId(), "webhook");
            case "FAILED", "DECLINED" -> markPaymentFailed(
                    payment, request.getFailureReason() != null ? request.getFailureReason() : "Payment failed", "webhook");
            case "CANCELLED" -> markPaymentCancelled(payment, "webhook");
            default -> throw new BadRequestException("Unsupported webhook status: " + status);
        };
    }

    @Transactional
    public PaymentResponse verifyPayment(UUID paymentId, boolean approved, String adminNote) {
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }

        if (!approved) {
            return markPaymentFailed(payment,
                    adminNote != null ? adminNote : "Rejected by admin", "admin");
        }

        return completePaymentSuccess(payment, "TXN" + System.currentTimeMillis(), "admin");
    }

    @Transactional(readOnly = true)
    public java.util.List<PaymentResponse> getAllPayments() {
        return paymentRepository.findAllByOrderByCreatedAtDesc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    protected PaymentResponse completePaymentSuccess(Payment payment, String transactionId, String actor) {
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        if (isExpired(payment)) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            payment.setFailureReason("Payment window expired");
            paymentRepository.save(payment);
            throw new BadRequestException("Payment window has expired");
        }

        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        payment.setVerificationStatus(Payment.VerificationStatus.VERIFIED);
        payment.setPaidAt(LocalDateTime.now());
        if (transactionId != null) {
            payment.setTransactionId(transactionId);
        } else if (payment.getTransactionId() == null) {
            payment.setTransactionId("TXN" + System.currentTimeMillis());
        }
        paymentRepository.save(payment);
        addLog(payment, "PAYMENT_SUCCESS", "Payment confirmed via " + actor, actor);

        Order order = orderService.createOrderFromSession(payment.getCheckoutSession().getId());
        payment.setOrder(order);
        paymentRepository.save(payment);

        emailService.sendPaymentConfirmationEmail(order.getUser(), order, payment);
        auditService.log("PAYMENT_SUCCESS", "Payment", payment.getId().toString(),
                "Order: " + order.getOrderNumber());

        return toResponse(payment);
    }

    private PaymentResponse markPaymentFailed(Payment payment, String reason, String actor) {
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        payment.setStatus(Payment.PaymentStatus.FAILED);
        payment.setVerificationStatus(Payment.VerificationStatus.REJECTED);
        payment.setFailureReason(reason);
        paymentRepository.save(payment);
        addLog(payment, "PAYMENT_FAILED", reason, actor);
        emailService.sendPaymentFailedEmail(payment.getCheckoutSession().getUser(), payment);
        return toResponse(payment);
    }

    private PaymentResponse markPaymentCancelled(Payment payment, String actor) {
        if (payment.getStatus() == Payment.PaymentStatus.SUCCESS) {
            return toResponse(payment);
        }
        payment.setStatus(Payment.PaymentStatus.CANCELLED);
        payment.setFailureReason("Payment cancelled");
        paymentRepository.save(payment);
        addLog(payment, "PAYMENT_CANCELLED", "Payment cancelled", actor);
        return toResponse(payment);
    }

    private void expireIfNeeded(Payment payment) {
        if (payment.getStatus() == Payment.PaymentStatus.PENDING && isExpired(payment)) {
            payment.setStatus(Payment.PaymentStatus.EXPIRED);
            payment.setFailureReason("Payment timeout — QR code expired");
            paymentRepository.save(payment);
            addLog(payment, "PAYMENT_EXPIRED", "Payment window expired", "system");
        }
    }

    private boolean isExpired(Payment payment) {
        return payment.getExpiresAt() != null && payment.getExpiresAt().isBefore(LocalDateTime.now());
    }

    private boolean verifyWebhookSignature(PaymentWebhookRequest request) {
        if (request.getSignature() == null || request.getSignature().isBlank()) {
            return false;
        }
        try {
            String payload = request.getPaymentId() + ":" + request.getStatus()
                    + ":" + (request.getTransactionId() != null ? request.getTransactionId() : "");
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            String expected = HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
            return expected.equalsIgnoreCase(request.getSignature());
        } catch (Exception e) {
            return false;
        }
    }

    private Payment getPaymentForUser(UUID paymentId) {
        User user = getCurrentUser();
        Payment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("Payment not found"));

        if (!payment.getCheckoutSession().getUser().getId().equals(user.getId())
                && user.getRole() != User.Role.ADMIN) {
            throw new BadRequestException("Access denied");
        }
        return payment;
    }

    private void addLog(Payment payment, String event, String details, String actor) {
        PaymentLog log = PaymentLog.builder()
                .payment(payment)
                .event(event)
                .details(details)
                .actor(actor)
                .build();
        paymentLogRepository.save(log);
        payment.getLogs().add(log);
    }

    private Payment.PaymentProvider mapProvider(Payment.PaymentMethod method) {
        return switch (method) {
            case RAZORPAY -> Payment.PaymentProvider.RAZORPAY;
            case STRIPE -> Payment.PaymentProvider.STRIPE;
            case COD -> Payment.PaymentProvider.MANUAL;
            default -> Payment.PaymentProvider.UPI_QR;
        };
    }

    private PaymentStatusResponse toStatusResponse(Payment payment) {
        return PaymentStatusResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderNumber(payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null)
                .failureReason(payment.getFailureReason())
                .build();
    }

    public PaymentResponse toResponse(Payment payment) {
        return PaymentResponse.builder()
                .id(payment.getId())
                .checkoutSessionId(payment.getCheckoutSession() != null ? payment.getCheckoutSession().getId() : null)
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .orderNumber(payment.getOrder() != null ? payment.getOrder().getOrderNumber() : null)
                .method(payment.getMethod())
                .status(payment.getStatus())
                .verificationStatus(payment.getVerificationStatus())
                .provider(payment.getProvider())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .transactionId(payment.getTransactionId())
                .failureReason(payment.getFailureReason())
                .upiReference(payment.getUpiReference())
                .expiresAt(payment.getExpiresAt())
                .paidAt(payment.getPaidAt())
                .createdAt(payment.getCreatedAt())
                .qrImageUrl(payment.getMethod() == Payment.PaymentMethod.UPI_QR ? qrImageUrl : null)
                .logs(payment.getLogs().stream()
                        .map(l -> PaymentResponse.PaymentLogResponse.builder()
                                .event(l.getEvent())
                                .details(l.getDetails())
                                .actor(l.getActor())
                                .createdAt(l.getCreatedAt())
                                .build())
                        .collect(Collectors.toList()))
                .build();
    }
}
