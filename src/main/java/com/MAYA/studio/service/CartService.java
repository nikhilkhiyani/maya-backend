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
    
    @Transactional
    public CartResponse addToCart(UUID productId, Integer quantity) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        if (quantity <= 0) {
            throw new BadRequestException("Quantity must be positive");
        }
        
        if (product.getStock() < quantity) {
            throw new BadRequestException("Insufficient stock");
        }
        
        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .orElse(Cart.builder()
                        .user(user)
                        .product(product)
                        .quantity(0)
                        .build());
        
        cart.setQuantity(cart.getQuantity() + quantity);
        Cart savedCart = cartRepository.save(cart);
        return cartMapper.toResponse(savedCart);
    }
    
    @Transactional
    public CartResponse updateCartQuantity(UUID productId, Integer quantity) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        Cart cart = cartRepository.findByUserAndProduct(user, product)
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
    public void removeFromCart(UUID productId) {
        User user = getCurrentUser();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + productId));
        
        cartRepository.deleteByUserAndProduct(user, product);
    }
    
    @Transactional(readOnly = true)
    public List<CartResponse> getCart() {
        User user = getCurrentUser();
        return cartRepository.findByUser(user).stream()
                .map(cartMapper::toResponse)
                .collect(Collectors.toList());
    }
}
