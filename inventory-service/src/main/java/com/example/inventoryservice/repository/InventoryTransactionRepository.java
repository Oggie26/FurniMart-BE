package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InventoryTransactionRepository extends JpaRepository<InventoryTransaction, Long> {
    @Query("SELECT it FROM InventoryTransaction it WHERE it.productId = :productId AND it.warehouse.id = :warehouseId")
    List<InventoryTransaction> findByProductIdAndWarehouseId(@Param("productId") String productId, @Param("warehouseId") String warehouseId);

    // Các query bổ sung nếu cần, ví dụ: tìm theo userId
    List<InventoryTransaction> findByUserId(String userId);
}
