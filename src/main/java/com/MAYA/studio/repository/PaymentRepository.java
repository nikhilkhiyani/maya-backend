package com.MAYA.studio.repository;

import com.MAYA.studio.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {
    Optional<Payment> findByOrderId(UUID orderId);
    Optional<Payment> findByCheckoutSessionId(UUID checkoutSessionId);
    Optional<Payment> findTopByCheckoutSessionIdOrderByCreatedAtDesc(UUID checkoutSessionId);
    Optional<Payment> findByRazorpayOrderId(String razorpayOrderId);
    List<Payment> findAllByOrderByCreatedAtDesc();
    List<Payment> findByStatusOrderByCreatedAtDesc(Payment.PaymentStatus status);
}
