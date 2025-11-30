package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.enums.EnumRowLabel;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface LocationItemRepository extends JpaRepository<LocationItem, String> {
    Optional<LocationItem> findByIdAndIsDeletedFalse(String locationItemId);
    Optional<LocationItem> findFirstByZone_Warehouse_Id(String warehouseId);
    List<LocationItem> findByZoneIdAndIsDeletedFalse(String zoneId);

    @Query("""
        SELECT li
        FROM LocationItem li
        JOIN li.zone z
        JOIN z.warehouse w
        WHERE w.id = :warehouseId
          AND li.rowLabel = :rowLabel
          AND li.columnNumber = :columnNumber
          AND li.isDeleted = false
    """)
    Optional<LocationItem> findByWarehouseIdAndRowLabelAndColumnNumber(
            @Param("warehouseId") String warehouseId,
            @Param("rowLabel") Integer rowLabel,
            @Param("columnNumber") Integer columnNumber
    );

    Optional<LocationItem> findByZoneIdAndRowLabelAndColumnNumberAndIsDeletedFalse(
            String zoneId,
            Integer rowLabel,
            Integer columnNumber
    );
    @Query(value = """
        SELECT li.* 
        FROM location_item li
        JOIN zone z ON li.zone_id = z.id
        JOIN warehouse w ON z.warehouse_id = w.id
        WHERE li.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(li.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(li.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(z.zone_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            countQuery = """
        SELECT COUNT(*) 
        FROM location_item li
        JOIN zone z ON li.zone_id = z.id
        JOIN warehouse w ON z.warehouse_id = w.id
        WHERE li.is_deleted = false
        AND (
            :keyword IS NULL
            OR LOWER(li.code) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(li.description) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(z.zone_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            OR LOWER(w.warehouse_name) LIKE LOWER(CONCAT('%', :keyword, '%'))
        )
        """,
            nativeQuery = true)
    Page<LocationItem> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);
}
