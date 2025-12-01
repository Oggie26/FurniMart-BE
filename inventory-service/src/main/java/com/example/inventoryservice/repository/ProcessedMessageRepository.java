package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.ProcessedMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProcessedMessageRepository extends JpaRepository<ProcessedMessage, Long> {
    Optional<ProcessedMessage> findByOrderId(Long orderId);
    boolean existsByOrderId(Long orderId);
}





