package com.example.inventoryservice.service.inteface;


import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.InventoryTransactionResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse upsertInventory(
            String productId,
            String locationItemId,
            int quantity,
            int minQuantity,
            int maxQuantity
    );

    List<InventoryResponse> getInventoryByProduct(String productId);

    InventoryResponse increaseStock(String productId, String locationItemId, int amount);

    InventoryResponse decreaseStock(String productId, String locationItemId, int amount);

    boolean hasSufficientStock(String productId, String locationItemId, int requiredQty);

    boolean hasSufficientGlobalStock(String productId, int requiredQty);

    List<InventoryTransactionResponse> getTransactionHistory(String productId, String zoneId);

    List<InventoryResponse> getInventoryByZone(String zoneId);

    boolean checkZoneCapacity(String zoneId);
}

