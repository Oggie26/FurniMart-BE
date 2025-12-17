package com.example.orderservice.repository;

import com.example.orderservice.entity.Voucher;
import com.example.orderservice.enums.VoucherType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface VoucherRepository extends JpaRepository<Voucher, Integer> {
    
    Optional<Voucher> findByCodeAndIsDeletedFalse(String code);
    
    Optional<Voucher> findByIdAndIsDeletedFalse(Integer id);

    Optional<Voucher> findVoucherByOrderIdAndIsDeletedFalse(Long orderId);
    
    List<Voucher> findByStatusTrueAndIsDeletedFalse();
    
    List<Voucher> findByTypeAndStatusTrueAndIsDeletedFalse(VoucherType type);
    
    List<Voucher> findByOrderIdAndIsDeletedFalse(Long orderId);
    
    @Query("SELECT v FROM Voucher v WHERE v.status = true AND v.isDeleted = false AND " +
           "v.startDate <= :now AND v.endDate >= :now AND " +
           "(v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    List<Voucher> findActiveVouchers(@Param("now") LocalDateTime now);
    
    @Query("SELECT v FROM Voucher v WHERE v.code = :code AND v.status = true AND v.isDeleted = false AND " +
           "v.startDate <= :now AND v.endDate >= :now AND " +
           "(v.usageLimit IS NULL OR v.usedCount < v.usageLimit)")
    Optional<Voucher> findActiveVoucherByCode(@Param("code") String code, @Param("now") LocalDateTime now);
    
    @Query("SELECT v FROM Voucher v WHERE v.status = true AND v.isDeleted = false AND " +
           "v.startDate <= :now AND v.endDate >= :now AND " +
           "(v.usageLimit IS NULL OR v.usedCount < v.usageLimit) AND " +
           "(v.minimumOrderAmount IS NULL OR v.minimumOrderAmount <= :orderAmount)")
    List<Voucher> findApplicableVouchers(@Param("now") LocalDateTime now, @Param("orderAmount") Float orderAmount);
    
    @Query("SELECT v FROM Voucher v WHERE v.endDate < :now AND v.status = true AND v.isDeleted = false")
    List<Voucher> findExpiredVouchers(@Param("now") LocalDateTime now);
    
    boolean existsByCodeAndIsDeletedFalse(String code);
    
    @Query("SELECT COUNT(v) FROM Voucher v WHERE v.type = :type AND v.status = true AND v.isDeleted = false")
    Long countByType(@Param("type") VoucherType type);
}
