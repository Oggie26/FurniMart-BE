package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory,String> {

    List<Inventory> findAllByProductColorId(String productColorId);
    Optional<Inventory> findByProductColorId(String productColorId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i WHERE i.productColorId = :productColorId")
    int getTotalQuantityByProductColorId(String productColorId);

    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
            "JOIN i.locationItem li " +
            "JOIN li.zone z " +
            "WHERE z.warehouse.id = :warehouseId")
    Integer sumQuantityByWarehouseId(@Param("warehouseId") String warehouseId);


    @Query("SELECT i FROM Inventory i " +
            "JOIN i.locationItem li " +
            "WHERE li.zone.id = :zoneId")
    List<Inventory> findAllByLocationItem_Zone_ZoneId(@Param("zoneId") String zoneId);
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM Inventory i " +
            "JOIN i.locationItem li " +
            "WHERE li.zone.id = :zoneId")
    Integer sumQuantityByLocationItem_Zone_ZoneId(@Param("zoneId") String zoneId);
    Optional<Inventory> findByProductColorIdAndLocationItemId(String productColorId, String locationItemId);
    @Query("SELECT SUM(i.quantity) FROM Inventory i WHERE i.productColorId = :productColorId")
    Integer sumQuantityByProductId(@Param("productColorId") String productId);

    @Query("SELECT COALESCE(SUM(i.quantity),0) FROM Inventory i WHERE i.locationItem.zone.id = :zoneId")
    int sumQuantityByZoneId(@Param("zoneId") String zoneId);


    Optional<Inventory> findByLocationItem_IdAndProductColorId(String locationItemId, String productColorId);
    @Query(value = """
            SELECT * FROM inventory i
            WHERE i.status = 'ACTIVE'
              AND (
                   LOWER(i.productColorId_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            countQuery = """
            SELECT COUNT(*) FROM inventory i
            WHERE i.status = 'ACTIVE'
              AND (
                   LOWER(i.productColorId_id) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(i.id) LIKE LOWER(CONCAT('%', :keyword, '%'))
              )
            """,
            nativeQuery = true)
    Page<Inventory> searchByKeywordNative(@Param("keyword") String keyword, Pageable pageable);

}
