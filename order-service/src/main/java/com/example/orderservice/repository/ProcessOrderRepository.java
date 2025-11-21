package com.example.orderservice.repository;

import com.example.orderservice.entity.ProcessOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProcessOrderRepository extends JpaRepository<ProcessOrder, Long> {
    
    /**
     * Lấy lịch sử status của đơn hàng theo orderId, sắp xếp theo thời gian tạo (cũ nhất trước)
     */
    @Query("SELECT p FROM ProcessOrder p WHERE p.order.id = :orderId ORDER BY p.createdAt ASC")
    List<ProcessOrder> findByOrderIdOrderByCreatedAtAsc(@Param("orderId") Long orderId);
    
    /**
     * Lấy lịch sử status của đơn hàng theo orderId, sắp xếp theo thời gian tạo (mới nhất trước)
     */
    @Query("SELECT p FROM ProcessOrder p WHERE p.order.id = :orderId ORDER BY p.createdAt DESC")
    List<ProcessOrder> findByOrderIdOrderByCreatedAtDesc(@Param("orderId") Long orderId);
}
