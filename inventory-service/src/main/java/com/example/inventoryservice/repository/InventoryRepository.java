package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.TransferStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findAllByWarehouse_Id(String warehouseId);

    List<Inventory> findAllByWarehouse_IdAndPurposeAndTransferStatus(
            String warehouseId,
            EnumPurpose purpose,
            TransferStatus transferStatus
    );
}
