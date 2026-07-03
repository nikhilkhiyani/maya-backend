package com.MAYA.studio.service;

import com.MAYA.studio.dto.RazorpayOrderResponse;
import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.Payment;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.repository.OrderRepository;
import com.MAYA.studio.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;

    @Value("${razorpay.key-id:}")
    private String keyId;

    @Value("${razorpay.key-secret:}")
    private String keySecret;

    @Value("${razorpay.enabled:false}")
    private boolean razorpayEnabled;

    @Value("${razorpay.webhook-secret:}")
    private String webhookSecret;

    private final RestTemplate restTemplate = new RestTemplate();

    /** Whether Razorpay is configured and turned on. */
    public boolean isEnabled() {
        return razorpayEnabled && !keyId.isBlank() && !keySecret.isBlank();
    }

    public String getKeyId() {
        return keyId;
    }

    public int toPaise(BigDecimal amountInRupees) {
        return amountInRupees.multiply(BigDecimal.valueOf(100)).setScale(0, java.math.RoundingMode.HALF_UP).intValueExact();
    }

    /**
     * Creates an order on Razorpay via their REST API and returns the Razorpay order id.
     * Kept provider-agnostic so both the order-based and checkout-session-based flows can reuse it.
     */
    public String createApiOrder(BigDecimal amountInRupees, String receipt) {
        if (!isEnabled()) {
            throw new BadRequestException("Razorpay is not configured. Please choose another payment method.");
        }

        int amountInPaise = toPaise(amountInRupees);
        if (amountInPaise < 100) {
            throw new BadRequestException("Amount must be at least \u20B91 (100 paise)");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountInPaise);
        body.put("currency", "INR");
        // Razorpay caps the receipt field at 40 characters, so trim to stay within it.
        body.put("receipt", receipt != null && receipt.length() > 40 ? receipt.substring(0, 40) : receipt);
        body.put("payment_capture", 1);

        try {
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<Map> response = restTemplate.postForEntity(
                    "https://api.razorpay.com/v1/orders", request, Map.class);
            Map<String, Object> razorpayOrder = response.getBody();
            if (razorpayOrder == null || razorpayOrder.get("id") == null) {
                throw new BadRequestException("Razorpay did not return an order id");
            }
            return (String) razorpayOrder.get("id");
        } catch (org.springframework.web.client.HttpStatusCodeException e) {
            // Razorpay answered with a 4xx/5xx — the body carries the real reason
            // (e.g. authentication failed for bad keys). Surface it so it's diagnosable.
            String responseBody = e.getResponseBodyAsString();
            log.error("Razorpay rejected order creation ({}): {}", e.getStatusCode(), responseBody);
            if (e.getStatusCode().value() == 401) {
                throw new BadRequestException("Payment gateway authentication failed. Please verify the Razorpay API keys.");
            }
            throw new BadRequestException("Payment gateway error: " + extractRazorpayError(responseBody));
        } catch (org.springframework.web.client.RestClientException e) {
            // Couldn't even get a response (DNS, timeout, no network egress, etc.).
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw new BadRequestException("Could not reach the payment gateway. Please check the server's internet connection and try again.");
        }
    }

    /** Best-effort extraction of Razorpay's human-readable error description. */
    @SuppressWarnings("unchecked")
    private String extractRazorpayError(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "unknown error";
        }
        try {
            Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readValue(responseBody, Map.class);
            Object error = parsed.get("error");
            if (error instanceof Map<?, ?> errorMap && errorMap.get("description") != null) {
                return String.valueOf(errorMap.get("description"));
            }
        } catch (Exception ignored) {
            // fall through to returning the raw body
        }
        return responseBody;
    }

    public RazorpayOrderResponse createRazorpayOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        String razorpayOrderId = createApiOrder(order.getTotalAmount(), order.getOrderNumber());

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElse(Payment.builder()
                        .order(order)
                        .method(Payment.PaymentMethod.RAZORPAY)
                        .amount(order.getTotalAmount())
                        .build());

        payment.setRazorpayOrderId(razorpayOrderId);
        payment.setStatus(Payment.PaymentStatus.PENDING);
        paymentRepository.save(payment);

        return RazorpayOrderResponse.builder()
                .orderId(order.getId())
                .orderNumber(order.getOrderNumber())
                .razorpayOrderId(razorpayOrderId)
                .razorpayKeyId(keyId)
                .amount(order.getTotalAmount())
                .currency("INR")
                .build();
    }

    public void verifyPayment(UUID orderId, String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        if (!verifySignature(razorpayOrderId, razorpayPaymentId, razorpaySignature)) {
            throw new BadRequestException("Invalid payment signature");
        }

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        Payment payment = paymentRepository.findByOrderId(orderId)
                .orElseThrow(() -> new BadRequestException("Payment not found"));

        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setRazorpaySignature(razorpaySignature);
        payment.setStatus(Payment.PaymentStatus.SUCCESS);
        paymentRepository.save(payment);

        order.setPaymentStatus(Payment.PaymentStatus.SUCCESS);
        order.setStatus(Order.OrderStatus.PAYMENT_RECEIVED);
        orderRepository.save(order);
    }

    /**
     * Verifies a Razorpay webhook by comparing the HMAC-SHA256 (hex) of the raw request
     * body — keyed with the webhook secret — against the X-Razorpay-Signature header.
     */
    public boolean verifyWebhookSignature(String rawBody, String signatureHeader) {
        if (webhookSecret == null || webhookSecret.isBlank() || signatureHeader == null || rawBody == null) {
            return false;
        }
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(webhookSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(rawBody.getBytes(StandardCharsets.UTF_8));
            String expected = java.util.HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signatureHeader);
        } catch (Exception e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    public boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            // Razorpay signs "order_id|payment_id" with HMAC-SHA256 and returns it
            // HEX-encoded (NOT Base64). Compare hex to hex.
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = java.util.HexFormat.of().formatHex(hash);
            return expected.equalsIgnoreCase(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
