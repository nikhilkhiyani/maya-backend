package com.MAYA.studio.service;

import com.MAYA.studio.dto.WishlistResponse;
import com.MAYA.studio.entity.Product;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.entity.Wishlist;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.WishlistMapper;
import com.MAYA.studio.repository.ProductRepository;
import com.MAYA.studio.repository.UserRepository;
import com.MAYA.studio.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WishlistService {
    
    private final WishlistRepository wishlistRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final WishlistMapper wishlistMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String authUsername = authentication.getName();
        return userRepository.findByEmailOrPhone(authUsername)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    @Transactional
    public WishlistResponse addToWishlist(UUID productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (wishlistRepository.findByUserAndProduct(user, product).isPresent()) {
            throw new BadRequestException("Product already in wishlist");
        }
        
        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();
        
        Wishlist savedWishlist = wishlistRepository.save(wishlist);
        return wishlistMapper.toResponse(savedWishlist);
    }
    
    @Transactional
    public void removeFromWishlist(UUID productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        wishlistRepository.deleteByUserAndProduct(user, product);
    }
    
    @Transactional(readOnly = true)
    public List<WishlistResponse> getWishlist() {
        User user = getCurrentUser();
        return wishlistRepository.findByUser(user).stream()
                .map(wishlistMapper::toResponse)
                .collect(Collectors.toList());
    }
}
