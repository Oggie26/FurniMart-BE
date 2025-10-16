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

    List<InventoryResponse> getInventoryByProduct(String productColorId);

    InventoryResponse increaseStock(String productColorId, String locationItemId, int amount, String warehouseId);

    InventoryResponse decreaseStock(String productColorId, String locationItemId, int amount, String warehouseId);

    boolean hasSufficientStock(String productColorId, String locationItemId, int requiredQty);

    boolean hasSufficientGlobalStock(String productColorId, int requiredQty);

    List<InventoryTransactionResponse> getTransactionHistory(String productColorId, String zoneId);

    List<InventoryResponse> getInventoryByZone(String zoneId);

    boolean checkZoneCapacity(String zoneId);

    List<InventoryTransactionResponse> getAllTransactions();

    List<InventoryResponse> getAllInventory();

    int getTotalStockByProductColorId(String productColorId);

    InventoryResponse getInventoryById(String inventoryId);

    void transferInventory(String productColorId, String locationItemId, int quantity, String warehouse1_Id, String warehouse2_Id);
}

