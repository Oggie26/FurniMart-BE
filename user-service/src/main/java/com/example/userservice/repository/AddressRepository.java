package com.example.userservice.repository;

import com.example.userservice.entity.Address;
import com.example.userservice.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {

    List<Address> findByUser(User user);
    
    List<Address> findByUserId(String userId);
    
    Optional<Address> findByUserAndIsDefaultTrue(User user);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId AND a.isDefault = true")
    Optional<Address> findDefaultAddressByUserId(@Param("userId") String userId);
    
    @Query("SELECT a FROM Address a WHERE a.user.id = :userId")
    List<Address> findAllByUserId(@Param("userId") String userId);
    
    Page<Address> findByUser(User user, Pageable pageable);
    
    @Query("SELECT a FROM Address a WHERE a.city LIKE %:city% OR a.district LIKE %:district% OR a.ward LIKE %:ward%")
    List<Address> findByLocation(@Param("city") String city, @Param("district") String district, @Param("ward") String ward);
}
