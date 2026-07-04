package com.MAYA.studio.security;

import com.MAYA.studio.entity.User;
import com.MAYA.studio.repository.UserRepository;
import com.MAYA.studio.util.AuthIdentifierUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        String normalized = AuthIdentifierUtil.normalizeIdentifier(username);
        User user = userRepository.findByEmailOrPhone(normalized)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        String credential = user.canLoginWithPassword()
                ? user.getPassword()
                : "";

        return new org.springframework.security.core.userdetails.User(
                user.getAuthUsername(),
                credential,
                Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()))
        );
    }
}
