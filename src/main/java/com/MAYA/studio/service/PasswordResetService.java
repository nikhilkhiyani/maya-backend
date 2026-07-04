package com.MAYA.studio.service;

import com.MAYA.studio.entity.PasswordResetToken;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.repository.PasswordResetTokenRepository;
import com.MAYA.studio.repository.UserRepository;
import com.MAYA.studio.util.AuthIdentifierUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository tokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    @Transactional
    public void requestReset(String identifier) {
        String normalized = AuthIdentifierUtil.normalizeIdentifier(identifier);
        Optional<User> userOpt = userRepository.findByEmailOrPhone(normalized);

        if (userOpt.isEmpty()) {
            // Avoid leaking whether an account exists
            log.info("Password reset requested for unknown identifier");
            return;
        }

        User user = userOpt.get();
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            throw new BadRequestException(
                    "This account has no email on file. Add an email to your profile or contact support.");
        }
        if (!user.canLoginWithPassword() && user.getAuthProvider() == User.AuthProvider.GOOGLE) {
            throw new BadRequestException("This account uses Google sign-in. Sign in with Google instead.");
        }

        String token = UUID.randomUUID().toString();
        tokenRepository.save(PasswordResetToken.builder()
                .user(user)
                .token(token)
                .expiresAt(LocalDateTime.now().plusHours(1))
                .build());

        emailService.sendPasswordResetEmail(user, frontendUrl + "/reset-password?token=" + token);
        auditService.log("PASSWORD_RESET_REQUESTED", "User", user.getId().toString(), user.getEmail());
    }

    @Transactional
    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = tokenRepository.findByTokenAndUsedFalse(token)
                .orElseThrow(() -> new BadRequestException("Invalid or expired reset token"));

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Reset token has expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        if (user.getAuthProvider() == User.AuthProvider.GOOGLE && user.getGoogleId() != null) {
            // Allow password login alongside Google after reset
            user.setAuthProvider(User.AuthProvider.LOCAL);
        }
        userRepository.save(user);

        resetToken.setUsed(true);
        tokenRepository.save(resetToken);
        auditService.log("PASSWORD_RESET_COMPLETED", "User", user.getId().toString(), null);
    }
}
