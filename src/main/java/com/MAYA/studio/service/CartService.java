package com.MAYA.studio.service;

import com.MAYA.studio.dto.CartResponse;
import com.MAYA.studio.entity.Cart;
import com.MAYA.studio.entity.Product;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.exception.BadRequestException;
import com.MAYA.studio.exception.ResourceNotFoundException;
import com.MAYA.studio.mapper.CartMapper;
import com.MAYA.studio.repository.CartRepository;
import com.MAYA.studio.repository.ProductRepository;
import com.MAYA.studio.repository.UserRepository;
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
public class CartService {
    
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CartMapper cartMapper;
    
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }
    
    private String normalizeSize(String size) {
        return size == null ? "" : size.trim();
    }

    @Transactional
    public CartResponse addToCart(UUID productId, Integer quantity, String size) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        String normalizedSize = normalizeSize(size);
        
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        
        if (product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock");
        }
        
        Cart cart = cartRepository.findByUserAndProductAndSize(user, product, normalizedSize)
                .orElse(Cart.builder()
                        .user(user)
                        .product(product)
                        .size(normalizedSize)
                        .quantity(0)
                        .build());
        
        cart.setQuantity(cart.getQuantity() + quantity);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }
    
    @Transactional
    public CartResponse updateCartQuantity(UUID productId, Integer quantity, String size) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        String normalizedSize = normalizeSize(size);
        
        Cart cart = cartRepository.findByUserAndProductAndSize(user, product, normalizedSize)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item not found"));
        
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        
        if (product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock");
        }
        
        cart.setQuantity(quantity);
        Cart updatedCart = cartRepository.save(cart);
        return cartMapper.toResponse(updatedCart);
    }
    
    @Transactional
    public void removeFromCart(UUID productId, String size) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        cartRepository.deleteByUserAndProductAndSize(user, product, normalizeSize(size));
    }
    
    @Transactional(readOnly = true)
    public List<CartResponse> getCart() {
        User user = getCurrentUser();
        return cartRepository.findByUser(user).stream()
                .map(cartMapper::toResponse)
                .collect(Collectors.toList());
    }
}
