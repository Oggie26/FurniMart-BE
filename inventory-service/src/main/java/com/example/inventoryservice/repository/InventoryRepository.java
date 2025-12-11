package com.example.inventoryservice.repository;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface InventoryRepository extends JpaRepository<Inventory, Long> {

    List<Inventory> findAllByWarehouse_Id(String warehouseId);

//    List<Inventory> findAllByWarehouse_IdAndPurposeAndTransferStatus(
//            String warehouseId,
//            EnumPurpose purpose,
//            TransferStatus transferStatus
//    );
List<Inventory> findAllByWarehouse_IdAndPurpose(String warehouseId, EnumPurpose purpose);

    @Query("SELECT i FROM Inventory i LEFT JOIN FETCH i.inventoryItems it LEFT JOIN FETCH it.locationItem WHERE i.id = :id")
    Optional<Inventory> findByIdWithItems(@Param("id") Long id);

    @Query("SELECT i FROM Inventory i WHERE i.warehouse.id = :warehouseId AND i.type = 'RESERVE'    ")
    List<Inventory> findPendingReservations(@Param("warehouseId") String warehouseId);

    Inventory findByOrderId(Long orderId);

    Optional<Inventory> findByOrderIdAndWarehouseId(Long orderId, String warehouseId);
    List<Inventory> findAllByOrderId(Long orderId);

    List<Inventory> findAllByType(EnumTypes type);


}
