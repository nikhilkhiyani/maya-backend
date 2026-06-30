package com.MAYA.studio.repository;

import com.MAYA.studio.entity.PaymentLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface PaymentLogRepository extends JpaRepository<PaymentLog, UUID> {
    List<PaymentLog> findByPaymentIdOrderByCreatedAtDesc(UUID paymentId);
}
