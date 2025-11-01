package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.InventoryItemResponse;
import com.example.inventoryservice.response.InventoryResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse createOrUpdateInventory(InventoryRequest request);

    InventoryItemResponse addInventoryItem(InventoryItemRequest request);

    InventoryResponse importStock(InventoryItemRequest request);

    InventoryResponse exportStock(InventoryItemRequest request);

    void transferStock(TransferStockRequest request);

    InventoryResponse reserveStock(String productColorId, int quantity);

    InventoryResponse releaseReservedStock(String productColorId, int quantity);

    boolean hasSufficientStock(String productColorId, String warehouseId, int requiredQty);

    boolean hasSufficientGlobalStock(String productColorId, int requiredQty);

    int getTotalStockByProductColorId(String productColorId);

    int getAvailableStockByProductColorId(String productColorId);

    List<InventoryResponse> getInventoryByWarehouse(String warehouseId);

    List<InventoryResponse> getInventoryByZone(String zoneId);

    List<InventoryItemResponse> getInventoryItemsByProduct(String productColorId);

    List<InventoryItemResponse> getTransactionHistory(String productColorId, String zoneId);

    List<InventoryItemResponse> getAllInventoryItems();

    List<InventoryResponse> getAllInventories();

    InventoryResponse getInventoryById(Long inventoryId);

    boolean checkZoneCapacity(String zoneId, int additionalQty);
}
