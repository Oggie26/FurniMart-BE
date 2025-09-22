package com.example.inventoryservice.service.inteface;


import com.example.inventoryservice.response.InventoryResponse;

import java.util.List;

public interface InventoryService {
    InventoryResponse upsertInventory(String productId, String locationItemId, int quantity, int minQuantity, int maxQuantity);
    List<InventoryResponse> getInventoryByProduct(String productId);
    InventoryResponse increaseStock(String inventoryId, int amount);
    InventoryResponse decreaseStock(String inventoryId, int amount);
    boolean hasSufficientStock(String productId, int requiredQty);
}
