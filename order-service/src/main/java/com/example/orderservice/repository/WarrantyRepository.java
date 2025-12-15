package com.example.orderservice.repository;

import com.example.orderservice.entity.Warranty;
import com.example.orderservice.enums.WarrantyStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface WarrantyRepository extends JpaRepository<Warranty, Long> {

    List<Warranty> findByOrderIdAndIsDeletedFalse(Long orderId); // Deprecated: Use findByOrder_IdAndIsDeletedFalse instead
    List<Warranty> findByOrder_IdAndIsDeletedFalse(Long orderId);

    List<Warranty> findByOrderDetailIdAndIsDeletedFalse(Long orderDetailId);

    List<Warranty> findByCustomerIdAndIsDeletedFalse(String customerId);

    List<Warranty> findByStatusAndIsDeletedFalse(WarrantyStatus status);

    @Query("SELECT w FROM Warranty w WHERE w.customerId = :customerId AND w.isDeleted = false AND w.status = 'ACTIVE' AND w.warrantyEndDate > :now")
    List<Warranty> findActiveWarrantiesByCustomer(@Param("customerId") String customerId,
            @Param("now") LocalDateTime now);

    @Query("SELECT w FROM Warranty w WHERE w.order.id = :orderId AND w.orderDetailId = :orderDetailId AND w.isDeleted = false")
    Optional<Warranty> findByOrderIdAndOrderDetailId(@Param("orderId") Long orderId,
            @Param("orderDetailId") Long orderDetailId);

    @Query("SELECT w FROM Warranty w WHERE w.isDeleted = false AND w.warrantyEndDate < :now AND w.status = 'ACTIVE'")
    List<Warranty> findExpiredWarranties(@Param("now") LocalDateTime now);

    @Query("SELECT w FROM Warranty w WHERE w.productColorId = :productColorId AND w.customerId = :customerId AND w.isDeleted = false AND w.status = 'ACTIVE'")
    List<Warranty> findActiveWarrantiesByProductAndCustomer(@Param("productColorId") String productColorId,
            @Param("customerId") String customerId);

    Optional<Warranty> findByIdAndIsDeletedFalse(Long id);

    Page<Warranty> findByStoreIdAndIsDeletedFalse(String storeId, Pageable pageable);
}
