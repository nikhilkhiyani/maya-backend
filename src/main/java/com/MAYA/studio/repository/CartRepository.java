package com.MAYA.studio.repository;

import com.MAYA.studio.entity.Cart;
import com.MAYA.studio.entity.Product;
import com.MAYA.studio.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface CartRepository extends JpaRepository<Cart, UUID> {
    
    List<Cart> findByUser(User user);
    
    Optional<Cart> findByUserAndProduct(User user, Product product);

    Optional<Cart> findByUserAndProductAndSize(User user, Product product, String size);
    
    void deleteByUserAndProduct(User user, Product product);

    void deleteByUserAndProductAndSize(User user, Product product, String size);

    void deleteByUser(User user);
}
