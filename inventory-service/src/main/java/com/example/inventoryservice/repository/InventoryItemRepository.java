package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryItemRepository extends JpaRepository<InventoryItem, Long> {

    // 🔹 Tìm tất cả item theo productColorId
    List<InventoryItem> findAllByProductColorId(String productColorId);

    // 🔹 Tìm tất cả item của sản phẩm trong một kho cụ thể
    List<InventoryItem> findAllByProductColorIdAndInventory_Warehouse_Id(String productColorId, String warehouseId);

    // 🔹 Tìm tất cả item theo zone (khu vực)
    List<InventoryItem> findAllByLocationItem_Zone_Id(String zoneId);

    // 🔹 Tổng số lượng tồn thực tế của sản phẩm
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryItem i WHERE i.productColorId = :productColorId")
    int getTotalQuantityByProductColorId(@Param("productColorId") String productColorId);

    @Query("SELECT COALESCE(SUM(ii.quantity), 0) FROM InventoryItem ii WHERE ii.locationItem.id = :locationId")
    int sumQuantityByLocationItemId(@Param("locationId") String locationId);

    @Query("""
    SELECT COALESCE(SUM(ii.quantity), 0)
    FROM InventoryItem ii
    WHERE ii.locationItem.id = :locationItemId
""")
    int getActualStock(@Param("locationItemId") String locationItemId);

    @Query("""
    SELECT ii
    FROM InventoryItem ii
    JOIN FETCH ii.locationItem li
    JOIN FETCH li.zone z
    JOIN FETCH z.warehouse w
    WHERE w.storeId = :storeId
""")
    List<InventoryItem> findAllByStore(@Param("storeId") String storeId);


    @Query("""
    SELECT COALESCE(SUM(ii.quantity), 0)
    FROM InventoryItem ii
    WHERE ii.locationItem.id = :locationItemId
      AND ii.inventory.type = 'IMPORT'
""")
    int getImportStock(@Param("locationItemId") String locationItemId);

    @Query("""
    SELECT COALESCE(SUM(ii.quantity), 0)
    FROM InventoryItem ii
    WHERE ii.locationItem.id = :locationItemId
""")
    int sumQuantityByLocation(@Param("locationItemId") String locationItemId);

    @Query("""
    SELECT ii
    FROM InventoryItem ii
    JOIN FETCH ii.locationItem li
    JOIN FETCH li.zone z
    JOIN FETCH z.warehouse w
    WHERE ii.productColorId = :productColorId
""")
    List<InventoryItem> findFullByProductColorId(@Param("productColorId") String productColorId);

    @Query("""
    SELECT ii 
    FROM InventoryItem ii 
    JOIN ii.locationItem li 
    JOIN li.zone z 
    JOIN z.warehouse w 
    WHERE ii.productColorId = :productColorId
      AND w.id = :warehouseId
""")
    List<InventoryItem> findFullByProductColorIdAndWarehouseId(String productColorId, String warehouseId);
    // 🔹 Tổng số lượng đang được giữ (reserve)
    @Query("SELECT COALESCE(SUM(i.reservedQuantity), 0) FROM InventoryItem i WHERE i.productColorId = :productColorId")
    int getTotalReservedQuantityByProductColorId(@Param("productColorId") String productColorId);

    // 🔹 Tổng tồn trong 1 kho
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryItem i WHERE i.inventory.warehouse.id = :warehouseId")
    int sumQuantityByWarehouseId(@Param("warehouseId") String warehouseId);

    // 🔹 Tổng tồn trong 1 zone
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryItem i WHERE i.locationItem.zone.id = :zoneId")
    int sumQuantityByZoneId(@Param("zoneId") String zoneId);

    @Query("SELECT i FROM InventoryItem i " +
            "WHERE i.productColorId = :pci AND i.inventory.warehouse.id = :wid " +
            "ORDER BY i.reservedQuantity DESC, i.quantity DESC") // <- Ưu tiên thằng có Reserved
    List<InventoryItem> findItemsForExport(@Param("pci") String pci, @Param("wid") String wid);
}
