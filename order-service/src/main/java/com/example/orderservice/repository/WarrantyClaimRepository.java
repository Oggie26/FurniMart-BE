package com.example.orderservice.repository;

import com.example.orderservice.entity.WarrantyClaim;
import com.example.orderservice.enums.WarrantyClaimStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {
    
    List<WarrantyClaim> findByWarrantyIdAndIsDeletedFalse(Long warrantyId);
    
    List<WarrantyClaim> findByCustomerIdAndIsDeletedFalse(String customerId);
    
    List<WarrantyClaim> findByStatusAndIsDeletedFalse(WarrantyClaimStatus status);
    
    @Query("SELECT wc FROM WarrantyClaim wc WHERE wc.warrantyId = :warrantyId AND wc.isDeleted = false ORDER BY wc.claimDate DESC")
    List<WarrantyClaim> findByWarrantyIdOrderByClaimDateDesc(@Param("warrantyId") Long warrantyId);
    
    @Query("SELECT wc FROM WarrantyClaim wc WHERE wc.customerId = :customerId AND wc.isDeleted = false ORDER BY wc.claimDate DESC")
    List<WarrantyClaim> findByCustomerIdOrderByClaimDateDesc(@Param("customerId") String customerId);
    
    @Query("SELECT COUNT(wc) FROM WarrantyClaim wc WHERE wc.warrantyId = :warrantyId AND wc.isDeleted = false")
    Long countClaimsByWarrantyId(@Param("warrantyId") Long warrantyId);
    
    Optional<WarrantyClaim> findByIdAndIsDeletedFalse(Long id);
    
    List<WarrantyClaim> findByIsDeletedFalse();
}
