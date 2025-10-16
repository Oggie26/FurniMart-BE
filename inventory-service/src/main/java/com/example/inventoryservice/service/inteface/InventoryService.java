package com.example.inventoryservice.service.inteface;


import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.InventoryTransactionResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse upsertInventory(
            String productColorId,
            String locationItemId,
            int quantity,
            int minQuantity,
            int maxQuantity
    );

    InventoryResponse increaseStock(String productColorId, String locationItemId, int amount, String warehouseId);

    InventoryResponse decreaseStock(String productColorId, String locationItemId, int amount, String warehouseId);

    void transferInventory(String productColorId, String locationItemId, int quantity, String warehouse1_Id, String warehouse2_Id);
    InventoryResponse reserveStock(String productColorId, int amount);


    InventoryResponse releaseStock(String productColorId, int amount);

    boolean hasSufficientStock(String productColorId, String locationItemId, int requiredQty);

    boolean hasSufficientGlobalStock(String productColorId, int requiredQty);

    int getTotalStockByProductColorId(String productColorId);


    int getTotalAvailableStockByProductColorId(String productColorId);

    List<InventoryResponse> getInventoryByProduct(String productColorId);

    List<InventoryTransactionResponse> getTransactionHistory(String productColorId, String zoneId);

    List<InventoryResponse> getInventoryByZone(String zoneId);

    boolean checkZoneCapacity(String zoneId);

    List<InventoryTransactionResponse> getAllTransactions();

    List<InventoryResponse> getAllInventory();

    InventoryResponse getInventoryById(String inventoryId);
}
