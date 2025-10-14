package com.example.orderservice.repository;

import com.example.orderservice.entity.OrderDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderDetailRepository extends JpaRepository<OrderDetail, Long> {
    
    List<OrderDetail> findByOrderIdAndIsDeletedFalse(Long orderId);
}
