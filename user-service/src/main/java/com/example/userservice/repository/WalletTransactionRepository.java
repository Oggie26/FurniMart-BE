package com.example.userservice.repository;

import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.enums.WalletTransactionType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Repository
public interface WalletTransactionRepository extends JpaRepository<WalletTransaction, String> {

    Optional<WalletTransaction> findByCodeAndIsDeletedFalse(String code);

    Optional<WalletTransaction> findByIdAndIsDeletedFalse(String id);

    List<WalletTransaction> findByWalletIdAndIsDeletedFalseOrderByCreatedAtDesc(String walletId);

    Page<WalletTransaction> findByWalletIdAndIsDeletedFalseOrderByCreatedAtDesc(String walletId, Pageable pageable);

    List<WalletTransaction> findByWalletIdAndStatusAndIsDeletedFalse(String walletId, WalletTransactionStatus status);

    List<WalletTransaction> findByWalletIdAndTypeAndIsDeletedFalse(String walletId, WalletTransactionType type);

    @Query("SELECT wt FROM WalletTransaction wt WHERE wt.walletId = :walletId AND wt.createdAt BETWEEN :startDate AND :endDate AND wt.isDeleted = false ORDER BY wt.createdAt DESC")
    List<WalletTransaction> findByWalletIdAndDateRange(@Param("walletId") String walletId,
            @Param("startDate") Date startDate,
            @Param("endDate") Date endDate);

    @Query("SELECT SUM(wt.amount) FROM WalletTransaction wt WHERE wt.walletId = :walletId AND wt.type = :type AND wt.status = 'COMPLETED' AND wt.isDeleted = false")
    Double getTotalAmountByWalletIdAndType(@Param("walletId") String walletId,
            @Param("type") WalletTransactionType type);

    boolean existsByCodeAndIsDeletedFalse(String code);

    Optional<WalletTransaction> findByReferenceIdAndIsDeletedFalse(String referenceId);

    Page<WalletTransaction> findByTypeAndIsDeletedFalseOrderByCreatedAtDesc(WalletTransactionType type,
            Pageable pageable);

    Page<WalletTransaction> findByTypeInAndIsDeletedFalseOrderByCreatedAtDesc(List<WalletTransactionType> types,
            Pageable pageable);
}
