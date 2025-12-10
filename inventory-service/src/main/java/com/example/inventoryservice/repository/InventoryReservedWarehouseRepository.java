package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservedWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryReservedWarehouseRepository extends JpaRepository<InventoryReservedWarehouse,Long> {
    List<InventoryReservedWarehouse> findByOrderId(Long orderId);
}
