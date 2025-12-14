package com.example.orderservice.repository;

import com.example.orderservice.entity.WarrantyClaimDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarrantyClaimDetailRepository extends JpaRepository<WarrantyClaimDetail, Long> {
    List<WarrantyClaimDetail> findByWarrantyClaimId(Long warrantyClaimId);
}
