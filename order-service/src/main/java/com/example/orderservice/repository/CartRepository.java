package com.example.orderservice.repository;

import com.example.orderservice.entity.Cart;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;


public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByUserId(String userId);
    Optional<Cart> findById(Long id);
}

