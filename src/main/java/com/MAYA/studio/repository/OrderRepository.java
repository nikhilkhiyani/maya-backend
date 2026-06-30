package com.MAYA.studio.repository;

import com.MAYA.studio.entity.Order;
import com.MAYA.studio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, UUID> {

    List<Order> findByUserOrderByCreatedAtDesc(User user);

    Optional<Order> findByOrderNumberAndUser(String orderNumber, User user);
}
