package com.MAYA.studio.repository;

import com.MAYA.studio.entity.ExchangeRequest;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ExchangeRequestRepository extends JpaRepository<ExchangeRequest, UUID> {
    List<ExchangeRequest> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<ExchangeRequest> findByStatusOrderByCreatedAtDesc(ExchangeRequest.ExchangeStatus status);
    List<ExchangeRequest> findAllByOrderByCreatedAtDesc();
}
