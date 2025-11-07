package com.example.userservice.repository;

import com.example.userservice.entity.EmployeeStore;
import com.example.userservice.entity.EmployeeStoreId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmployeeStoreRepository extends JpaRepository<EmployeeStore, EmployeeStoreId> {

    @Query("SELECT es FROM EmployeeStore es WHERE es.employeeId = :employeeId AND es.storeId = :storeId AND es.isDeleted = false")
    Optional<EmployeeStore> findByEmployeeIdAndStoreId(@Param("employeeId") String employeeId, @Param("storeId") String storeId);

    @Query("SELECT es FROM EmployeeStore es WHERE es.employeeId = :employeeId AND es.isDeleted = false")
    List<EmployeeStore> findByEmployeeIdAndIsDeletedFalse(@Param("employeeId") String employeeId);

    @Query("SELECT es FROM EmployeeStore es WHERE es.employeeId = :employeeId AND es.storeId = :storeId AND es.isDeleted = false")
    Optional<EmployeeStore> findByEmployeeIdAndStoreIdAndIsDeletedFalse(@Param("employeeId") String employeeId, @Param("storeId") String storeId);

    @Query("SELECT es FROM EmployeeStore es WHERE es.storeId = :storeId AND es.isDeleted = false")
    List<EmployeeStore> findByStoreIdAndIsDeletedFalse(@Param("storeId") String storeId);

    void deleteByEmployeeId(String employeeId);
}

