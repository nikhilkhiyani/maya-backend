package com.MAYA.studio.repository;

import com.MAYA.studio.entity.OrderTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OrderTimelineRepository extends JpaRepository<OrderTimeline, UUID> {
    List<OrderTimeline> findByOrderIdOrderByCreatedAtAsc(UUID orderId);
}
