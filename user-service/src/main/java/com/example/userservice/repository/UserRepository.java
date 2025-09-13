package com.example.userservice.repository;

import com.example.userservice.entity.User;
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
}
