package com.example.orderservice.repository;

import com.example.orderservice.entity.ProcessOrder;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProcessOrderRepository extends JpaRepository<ProcessOrder, Long> {
}
