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

    private final RestTemplate restTemplate = new RestTemplate();

    public RazorpayOrderResponse createRazorpayOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new BadRequestException("Order not found"));

        if (!razorpayEnabled || keyId.isBlank() || keySecret.isBlank()) {
            throw new BadRequestException("Razorpay is not configured. Use Cash on Delivery.");
        }

        int amountInPaise = order.getTotalAmount().multiply(BigDecimal.valueOf(100)).intValue();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBasicAuth(keyId, keySecret);

        Map<String, Object> body = new HashMap<>();
        body.put("amount", amountInPaise);
        body.put("currency", "INR");
        body.put("receipt", order.getOrderNumber());

        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<Map> response = restTemplate.postForEntity(
                "https://api.razorpay.com/v1/orders", request, Map.class);

        Map<String, Object> razorpayOrder = response.getBody();
        String razorpayOrderId = (String) razorpayOrder.get("id");

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

    private boolean verifySignature(String orderId, String paymentId, String signature) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(keySecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] hash = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            String expected = Base64.getEncoder().encodeToString(hash);
            return expected.equals(signature);
        } catch (Exception e) {
            log.error("Signature verification failed: {}", e.getMessage());
            return false;
        }
    }
}
