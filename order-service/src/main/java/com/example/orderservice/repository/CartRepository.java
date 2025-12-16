package com.example.orderservice.repository;

import com.example.orderservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;

import java.util.Optional;


public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(String userId);
    @NonNull
    Optional<Cart> findById(@NonNull Long id);

    @Modifying
    @Query("DELETE FROM CartItem c WHERE c.cart.id = :cartId")
    void deleteAllByCartId(@Param("cartId") Long cartId);
}

