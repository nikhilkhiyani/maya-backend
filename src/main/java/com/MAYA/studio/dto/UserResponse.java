package com.MAYA.studio.dto;

import com.MAYA.studio.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserResponse {
    private UUID id;
    private String name;
    private String email;
    private String phone;
    private User.Role role;
    private User.AuthProvider authProvider;
    private LocalDateTime createdAt;
}
