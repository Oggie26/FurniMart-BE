//package com.example.userservice.repository;
//
//import com.example.userservice.entity.Wallet;
//import com.example.userservice.enums.WalletStatus;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//import java.util.Optional;
//
//@Repository
//public interface WalletRepository extends JpaRepository<Wallet, String> {
//
//    Optional<Wallet> findByCodeAndIsDeletedFalse(String code);
//
//    Optional<Wallet> findByUserIdAndIsDeletedFalse(String userId);
//
//    Optional<Wallet> findByIdAndIsDeletedFalse(String id);
//
//    List<Wallet> findByStatusAndIsDeletedFalse(WalletStatus status);
//
//    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId AND w.status = :status AND w.isDeleted = false")
//    Optional<Wallet> findByUserIdAndStatus(@Param("userId") String userId, @Param("status") WalletStatus status);
//
//    boolean existsByCodeAndIsDeletedFalse(String code);
//
//    boolean existsByUserIdAndIsDeletedFalse(String userId);
//
//    // Find wallet by userId including deleted ones (for restore purpose)
//    @Query("SELECT w FROM Wallet w WHERE w.userId = :userId")
//    Optional<Wallet> findByUserId(@Param("userId") String userId);
//}
