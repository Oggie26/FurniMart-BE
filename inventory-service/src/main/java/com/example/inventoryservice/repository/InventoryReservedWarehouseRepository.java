package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservedWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryReservedWarehouseRepository extends JpaRepository<InventoryReservedWarehouse,Long> {
    List<InventoryReservedWarehouse> findByOrderId(Long orderId);
    List<InventoryReservedWarehouse> findAllByWarehouseId(String warehouseId);

    @Query("SELECT rw FROM InventoryReservedWarehouse rw JOIN FETCH rw.inventory WHERE rw.warehouseId = :warehouseId")
    List<InventoryReservedWarehouse> findByWarehouseIdWithInventory(@Param("warehouseId") String warehouseId);
}
