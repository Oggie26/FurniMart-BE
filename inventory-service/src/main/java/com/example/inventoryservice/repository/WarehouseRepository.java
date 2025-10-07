package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Warehouse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface WarehouseRepository extends JpaRepository<Warehouse, String> {
    Optional<Warehouse> findByIdAndIsDeletedFalse(String id);
    Optional<Warehouse> findByStoreId(String storeId);

    @Query("SELECT w FROM Warehouse w WHERE w.storeId = :storeId")
    Optional<Warehouse> findByStoreIdWithZones(@Param("storeId") String storeId);
    Optional<Warehouse> findByWarehouseNameAndIsDeletedFalse(String warehouseName);
    @Query(value = """
        SELECT * FROM warehouse w
        WHERE w.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            countQuery = """
        SELECT COUNT(*) FROM warehouse w
        WHERE w.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            nativeQuery = true)
    Page<Warehouse> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);

}
