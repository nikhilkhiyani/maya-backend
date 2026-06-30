package com.MAYA.studio.repository;

import com.MAYA.studio.entity.Product;
import com.MAYA.studio.entity.User;
import com.MAYA.studio.entity.Wishlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface WishlistRepository extends JpaRepository<Wishlist, UUID> {
    
    List<Wishlist> findByUser(User user);
    
    Optional<Wishlist> findByUserAndProduct(User user, Product product);
    
    void deleteByUserAndProduct(User user, Product product);
}
