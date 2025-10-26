package com.example.userservice.repository;

import com.example.userservice.entity.UserStore;
import com.example.userservice.entity.UserStoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserStoreRepository extends JpaRepository<UserStore, UserStoreId> {

    List<UserStore> findByUserIdAndIsDeletedFalse(String userId);
    
    List<UserStore> findByStoreIdAndIsDeletedFalse(String storeId);
    
    Optional<UserStore> findByUserIdAndStoreIdAndIsDeletedFalse(String userId, String storeId);
    
    @Query("SELECT us FROM UserStore us WHERE us.userId = :userId AND us.storeId = :storeId AND us.isDeleted = false")
    Optional<UserStore> findByUserIdAndStoreId(@Param("userId") String userId, @Param("storeId") String storeId);
    
    void deleteByUserIdAndStoreId(String userId, String storeId);
    
    void deleteByUserId(String userId);

    List<UserStore> findByUserId(String userId);
    
    List<UserStore> findByStoreId(String storeId);
}
