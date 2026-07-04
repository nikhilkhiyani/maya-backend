package com.MAYA.studio.service;

import com.MAYA.studio.dto.AuthResponse;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.repository.UserRepository;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdToken;
import com.google.api.client.googleapis.auth.oauth2.GoogleIdTokenVerifier;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;

@Service
@RequiredArgsConstructor
@Slf4j
public class GoogleAuthService {

    private final UserRepository userRepository;
    private final AuthService authService;

    @Value("${google.client-id:}")
    private String googleClientId;

    @Transactional
    public AuthResponse authenticateWithGoogle(String idToken) {
        if (googleClientId == null || googleClientId.isBlank()) {
            throw new BadRequestException("Google sign-in is not configured");
        }

        GoogleIdTokenVerifier verifier = new GoogleIdTokenVerifier.Builder(
                new NetHttpTransport(), GsonFactory.getDefaultInstance())
                .setAudience(Collections.singletonList(googleClientId))
                .build();

        GoogleIdToken googleToken;
        try {
            googleToken = verifier.verify(idToken);
        } catch (Exception ex) {
            log.warn("Google token verification failed: {}", ex.getMessage());
            throw new BadRequestException("Invalid Google token");
        }

        if (googleToken == null) {
            throw new BadRequestException("Invalid Google token");
        }

        GoogleIdToken.Payload payload = googleToken.getPayload();
        String googleId = payload.getSubject();
        String rawEmail = payload.getEmail();
        String name = (String) payload.get("name");

        if (rawEmail == null || rawEmail.isBlank()) {
            throw new BadRequestException("Google account must have an email address");
        }

        final String email = rawEmail.toLowerCase();

        User user = userRepository.findByGoogleId(googleId)
                .or(() -> userRepository.findByEmail(email))
                .orElse(null);

        if (user == null) {
            user = User.builder()
                    .name(name != null && !name.isBlank() ? name : email.split("@")[0])
                    .email(email)
                    .googleId(googleId)
                    .authProvider(User.AuthProvider.GOOGLE)
                    .role(User.Role.USER)
                    .build();
        } else {
            if (user.getGoogleId() == null) {
                user.setGoogleId(googleId);
            }
            if (user.getAuthProvider() == User.AuthProvider.LOCAL && user.canLoginWithPassword()) {
                // Keep LOCAL so password login still works; googleId enables Google login too
            } else {
                user.setAuthProvider(User.AuthProvider.GOOGLE);
            }
            if (name != null && !name.isBlank()) {
                user.setName(name);
            }
        }

        userRepository.save(user);
        return authService.buildAuthResponse(user);
    }
}
