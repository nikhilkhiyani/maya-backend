package com.MAYA.studio.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(unique = true)
    private String email;

    @Column(unique = true)
    private String phone;

    private String password;

    @Column(unique = true)
    private String googleId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private AuthProvider authProvider = AuthProvider.LOCAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    public enum Role {
        USER, ADMIN
    }

    public enum AuthProvider {
        LOCAL, GOOGLE
    }

    /** Canonical username for JWT and Spring Security (email preferred, else phone). */
    public String getAuthUsername() {
        if (email != null && !email.isBlank()) {
            return email;
        }
        return phone;
    }

    public boolean canLoginWithPassword() {
        return password != null && !password.isBlank();
    }
}
