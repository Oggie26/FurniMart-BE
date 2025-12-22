package com.example.orderservice.repository;

import com.example.orderservice.entity.WarrantyClaim;
import com.example.orderservice.enums.WarrantyActionType;
import com.example.orderservice.enums.WarrantyClaimStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaim, Long> {

    // List<WarrantyClaim> findByWarrantyIdAndIsDeletedFalse(Long warrantyId); // Removed/Updated

    List<WarrantyClaim> findByCustomerIdAndIsDeletedFalse(String customerId);

    List<WarrantyClaim> findByStatusAndIsDeletedFalse(WarrantyClaimStatus status);

    @Query("SELECT DISTINCT wc FROM WarrantyClaim wc JOIN wc.claimDetails d WHERE d.warranty.id = :warrantyId AND wc.isDeleted = false ORDER BY wc.claimDate DESC")
    List<WarrantyClaim> findByWarrantyIdOrderByClaimDateDesc(@Param("warrantyId") Long warrantyId);

    @Query("SELECT wc FROM WarrantyClaim wc WHERE wc.customerId = :customerId AND wc.isDeleted = false ORDER BY wc.claimDate DESC")
    List<WarrantyClaim> findByCustomerIdOrderByClaimDateDesc(@Param("customerId") String customerId);

    @Query("SELECT COUNT(d) FROM WarrantyClaimDetail d WHERE d.warranty.id = :warrantyId")
    Long countClaimsByWarrantyId(@Param("warrantyId") Long warrantyId);

    Optional<WarrantyClaim> findByIdAndIsDeletedFalse(Long id);

    List<WarrantyClaim> findByIsDeletedFalse();

    Long countByActionTypeAndIsDeletedFalse(WarrantyActionType actionType);

    @Query("SELECT SUM(wc.repairCost) FROM WarrantyClaim wc WHERE wc.isDeleted = false")
    Double sumRepairCost();

    @Query("SELECT SUM(wc.refundAmount) FROM WarrantyClaim wc WHERE wc.isDeleted = false")
    Double sumRefundAmount();

    Long countByStatusAndIsDeletedFalse(WarrantyClaimStatus status);

    @Query("SELECT wc FROM WarrantyClaim wc WHERE wc.orderId IN (SELECT o.id FROM Order o WHERE o.storeId = :storeId AND o.isDeleted = false) AND wc.isDeleted = false ORDER BY wc.claimDate DESC")
    Page<WarrantyClaim> findByStoreIdOrderByClaimDateDesc(@Param("storeId") String storeId, Pageable pageable);

    @Query("SELECT DISTINCT wc FROM WarrantyClaim wc JOIN wc.claimDetails d WHERE d.warranty.orderDetailId = :orderDetailId AND wc.status = :status AND wc.isDeleted = false")
    List<WarrantyClaim> findPendingClaimsByOrderDetailId(@Param("orderDetailId") Long orderDetailId, @Param("status") WarrantyClaimStatus status);

    @Query("SELECT DISTINCT wc FROM WarrantyClaim wc JOIN wc.claimDetails d WHERE d.warranty.orderDetailId = :orderDetailId AND wc.status IN :statuses AND wc.isDeleted = false")
    List<WarrantyClaim> findActiveClaimsByOrderDetailId(@Param("orderDetailId") Long orderDetailId, @Param("statuses") List<WarrantyClaimStatus> statuses);
}
