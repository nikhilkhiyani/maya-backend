package com.MAYA.studio.service;

import com.MAYA.studio.dto.UserResponse;
import com.MAYA.studio.dto.UserUpdateRequest;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.UserMapper;
import com.MAYA.studio.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {
    
    private final UserRepository userRepository;
    private final UserMapper userMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    public UserResponse getProfile() {
        User user = getCurrentUser();
        return userMapper.toResponse(user);
    }
    
    @Transactional
    public UserResponse updateProfile(UserUpdateRequest request) {
        User user = getCurrentUser();
        user.setName(request.getName());
        User updatedUser = userRepository.save(user);
        return userMapper.toResponse(updatedUser);
    }
    
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll().stream()
                .map(userMapper::toResponse)
                .collect(Collectors.toList());
    }
}
