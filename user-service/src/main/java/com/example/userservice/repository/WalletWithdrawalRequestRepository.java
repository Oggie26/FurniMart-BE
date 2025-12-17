package com.example.userservice.repository;

import com.example.userservice.entity.WalletWithdrawalRequest;
import com.example.userservice.enums.WithdrawalRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WalletWithdrawalRequestRepository extends JpaRepository<WalletWithdrawalRequest, String> {
    
    Optional<WalletWithdrawalRequest> findByCodeAndIsDeletedFalse(String code);
    
    Optional<WalletWithdrawalRequest> findByIdAndIsDeletedFalse(String id);
    
    List<WalletWithdrawalRequest> findByUserIdAndIsDeletedFalseOrderByCreatedAtDesc(String userId);
    
    List<WalletWithdrawalRequest> findByWalletIdAndIsDeletedFalseOrderByCreatedAtDesc(String walletId);
    
    List<WalletWithdrawalRequest> findByStatusAndIsDeletedFalseOrderByCreatedAtDesc(WithdrawalRequestStatus status);
    
    @Query("SELECT w FROM WalletWithdrawalRequest w WHERE w.status = :status AND w.isDeleted = false ORDER BY w.createdAt DESC")
    List<WalletWithdrawalRequest> findAllPendingApproval(@Param("status") WithdrawalRequestStatus status);
    
    boolean existsByCodeAndIsDeletedFalse(String code);
}
