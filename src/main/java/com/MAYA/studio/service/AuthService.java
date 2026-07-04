package com.MAYA.studio.service;

import com.MAYA.studio.dto.*;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.repository.UserRepository;
import com.MAYA.studio.security.JwtUtil;
import com.MAYA.studio.util.AuthIdentifierUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final EmailService emailService;

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = request.getEmail() != null && !request.getEmail().isBlank()
                ? request.getEmail().trim().toLowerCase()
                : null;
        String phone = request.getPhone() != null && !request.getPhone().isBlank()
                ? AuthIdentifierUtil.normalizePhone(request.getPhone())
                : null;

        if (email == null && phone == null) {
            throw new BadRequestException("Email or phone number is required");
        }
        if (email != null && userRepository.existsByEmail(email)) {
            throw new BadRequestException("Email already registered");
        }
        if (phone != null && userRepository.existsByPhone(phone)) {
            throw new BadRequestException("Phone number already registered");
        }

        User user = User.builder()
                .name(request.getName())
                .email(email)
                .phone(phone)
                .password(passwordEncoder.encode(request.getPassword()))
                .authProvider(User.AuthProvider.LOCAL)
                .role(User.Role.USER)
                .build();

        userRepository.save(user);

        if (email != null) {
            emailService.sendWelcomeEmail(user);
        }

        return buildAuthResponse(user);
    }

    public AuthResponse login(AuthRequest request) {
        String identifier = AuthIdentifierUtil.normalizeIdentifier(request.getIdentifier());
        User user = userRepository.findByEmailOrPhone(identifier)
                .orElseThrow(() -> new BadRequestException("Invalid credentials"));

        if (!user.canLoginWithPassword()) {
            if (user.getAuthProvider() == User.AuthProvider.GOOGLE) {
                throw new BadRequestException("This account uses Google sign-in. Please continue with Google.");
            }
            throw new BadRequestException("Invalid credentials");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(user.getAuthUsername(), request.getPassword())
            );
        } catch (BadCredentialsException ex) {
            throw new BadRequestException("Invalid credentials");
        }

        return buildAuthResponse(user);
    }

    public AuthResponse buildAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getAuthUsername());
        String token = jwtUtil.generateToken(userDetails, user.getRole().name());

        UserResponse userResponse = UserResponse.builder()
                .id(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .authProvider(user.getAuthProvider())
                .createdAt(user.getCreatedAt())
                .build();

        return AuthResponse.builder()
                .token(token)
                .user(userResponse)
                .build();
    }
}
