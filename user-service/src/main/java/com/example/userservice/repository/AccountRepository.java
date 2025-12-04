package com.example.userservice.repository;

import com.example.userservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByEmailAndIsDeletedFalse(String email);
    Optional<Account> findByIdAndIsDeletedFalse(String id);
    
    /**
     * Find all accounts with User and Employee eagerly loaded
     * Uses LEFT JOIN FETCH to avoid N+1 query problem
     * Only returns non-deleted accounts
     */
    @Query("SELECT DISTINCT a FROM Account a " +
           "LEFT JOIN FETCH a.user u " +
           "LEFT JOIN FETCH a.employee e " +
           "WHERE a.isDeleted = false")
    List<Account> findAllWithUserAndEmployee();

    Account findByUsername(String username);
}
