package com.example.orderservice.repository;

import com.example.orderservice.entity.Order;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<Order, Long> {
    Optional<Order> findByUserId(String userId);
    Optional<Order> findByIdAndIsDeletedFalse(Long orderId);
    Optional<Order> findByStoreId(String storeId);
}
