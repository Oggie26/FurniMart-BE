package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.InventoryReservedWarehouse;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InventoryReservedWarehouseRepository extends JpaRepository<InventoryReservedWarehouse,Long> {
}
