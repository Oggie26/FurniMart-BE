package com.example.userservice.repository;

import com.example.userservice.entity.User;
import feign.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,String> {
    Optional<User> findByEmailAndIsDeletedFalse(String email);
    Optional<User> findByPhoneAndIsDeletedFalse(String phone);
    Optional<User> findByUsernameAndIsDeletedFalse(String username);
    Optional<User> findUserByUsername(String username);
    Optional<User> findByIdAndIsDeletedFalse(String id);
    User findByUsernameAndIsDeletedFalse(User user);
}
