package com.example.userservice.repository;

import com.example.userservice.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, String> {
    Optional<Account> findByEmailAndIsDeletedFalse(String email);
    Optional<Account> findByIdAndIsDeletedFalse(String id);
}
