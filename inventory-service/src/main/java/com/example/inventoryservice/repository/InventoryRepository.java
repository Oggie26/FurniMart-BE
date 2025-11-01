package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    // 🔹 Lấy tất cả phiếu theo kho
    List<Inventory> findAllByWarehouse_Id(String warehouseId);
}
