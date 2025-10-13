package com.example.userservice.repository;

import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByIdAndIsDeletedFalse(String id);

    Optional<User> findByAccountIdAndIsDeletedFalse(String accountId);
    
    Optional<User> findByPhoneAndIsDeletedFalse(String phone);
    
    @Query("SELECT u FROM User u WHERE u.account.email = :email AND u.isDeleted = false")
    Optional<User> findByEmailAndIsDeletedFalse(@Param("email") String email);
    
    List<User> findByStatusAndIsDeletedFalse(EnumStatus status);
    
    Page<User> findByIsDeletedFalse(Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.isDeleted = false AND " +
           "(LOWER(u.fullName) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.phone) LIKE LOWER(CONCAT('%', :searchTerm, '%')) OR " +
           "LOWER(u.account.email) LIKE LOWER(CONCAT('%', :searchTerm, '%')))")
    Page<User> searchUsers(@Param("searchTerm") String searchTerm, Pageable pageable);
    
    List<User> findByIsDeletedFalse();
    
    // Employee-related queries
    @Query("SELECT u FROM User u WHERE u.account.role IN :roles AND u.isDeleted = false")
    List<User> findEmployeesByRoles(@Param("roles") List<EnumRole> roles);
    
    @Query("SELECT u FROM User u WHERE u.account.role = :role AND u.isDeleted = false")
    List<User> findEmployeesByRole(@Param("role") EnumRole role);
    
    @Query("SELECT u FROM User u WHERE u.account.role IN :roles AND u.isDeleted = false")
    Page<User> findEmployeesByRoles(@Param("roles") List<EnumRole> roles, Pageable pageable);
    
    @Query("SELECT u FROM User u WHERE u.account.role = :role AND u.isDeleted = false")
    Page<User> findEmployeesByRole(@Param("role") EnumRole role, Pageable pageable);
    
    @Query("SELECT u FROM User u JOIN u.userStores us WHERE us.storeId = :storeId AND u.account.role IN :roles AND u.isDeleted = false")
    List<User> findEmployeesByStoreIdAndRoles(@Param("storeId") String storeId, @Param("roles") List<EnumRole> roles);
    
    @Query("SELECT u FROM User u JOIN u.userStores us WHERE us.storeId = :storeId AND u.account.role = :role AND u.isDeleted = false")
    List<User> findEmployeesByStoreIdAndRole(@Param("storeId") String storeId, @Param("role") EnumRole role);
}
