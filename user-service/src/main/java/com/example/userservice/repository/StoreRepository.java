package com.example.userservice.repository;

import com.example.userservice.entity.Store;
import com.example.userservice.enums.EnumStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface StoreRepository extends JpaRepository<Store, String> {

    Optional<Store> findByIdAndIsDeletedFalse(String id);
    
    List<Store> findByStatusAndIsDeletedFalse(EnumStatus status);
    
    Page<Store> findByIsDeletedFalse(Pageable pageable);
    
    List<Store> findByIsDeletedFalse();
    
    List<Store> findByCityAndIsDeletedFalse(String city);
    
    @Query("SELECT s FROM Store s WHERE s.isDeleted = false AND " +
           "(LOWER(s.name) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.city) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(s.addressLine) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<Store> searchStores(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    @Query("SELECT s FROM Store s JOIN s.userStores us WHERE us.userId = :userId AND s.isDeleted = false")
    List<Store> findStoresByUserId(@Param("userId") String userId);
}
