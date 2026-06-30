package com.MAYA.studio.repository;

import com.MAYA.studio.entity.CheckoutSession;
import com.MAYA.studio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CheckoutSessionRepository extends JpaRepository<CheckoutSession, UUID> {
    List<CheckoutSession> findByUserAndStatus(User user, CheckoutSession.SessionStatus status);
    Optional<CheckoutSession> findByIdAndUser(UUID id, User user);
}
