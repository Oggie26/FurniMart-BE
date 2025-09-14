package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumZone;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ZoneRepository extends JpaRepository<Zone, String> {
    Optional<Zone> findByIdAndIsDeletedFalse(String id);
    Optional<Zone> findByZoneNameAndIsDeletedFalse(String zoneName);
    List<Zone> findByWarehouseIdAndIsDeletedFalse(String warehouseId);
    Optional<Zone> findByZoneCodeAndIsDeletedFalse(EnumZone zoneCode);
    @Query(value = """
        SELECT z.* 
        FROM zone z
        JOIN warehouse w ON z.warehouse_id = w.id
        WHERE z.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(z.zone_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(z.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            countQuery = """
        SELECT COUNT(*) 
        FROM zone z
        JOIN warehouse w ON z.warehouse_id = w.id
        WHERE z.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(z.zone_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(z.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            nativeQuery = true)
    Page<Zone> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);


}
