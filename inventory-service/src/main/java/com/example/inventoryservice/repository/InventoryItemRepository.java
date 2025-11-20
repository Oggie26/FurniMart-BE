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


    // 🔹 Tổng số lượng đang được giữ (reserve)
    @Query("SELECT COALESCE(SUM(i.reservedQuantity), 0) FROM InventoryItem i WHERE i.productColorId = :productColorId")
    int getTotalReservedQuantityByProductColorId(@Param("productColorId") String productColorId);

    // 🔹 Tổng tồn trong 1 kho
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryItem i WHERE i.inventory.warehouse.id = :warehouseId")
    int sumQuantityByWarehouseId(@Param("warehouseId") String warehouseId);

    // 🔹 Tổng tồn trong 1 zone
    @Query("SELECT COALESCE(SUM(i.quantity), 0) FROM InventoryItem i WHERE i.locationItem.zone.id = :zoneId")
    int sumQuantityByZoneId(@Param("zoneId") String zoneId);
}
