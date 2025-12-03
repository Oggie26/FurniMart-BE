package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.enums.TransferStatus;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.InventoryItemResponse;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.ProductLocationResponse;
import com.example.inventoryservice.response.ReserveStockResponse;

import java.util.List;

public interface InventoryService {

    InventoryResponse createOrUpdateInventory(InventoryRequest request);

    InventoryItemResponse addInventoryItem(InventoryItemRequest request, Long inventoryId);

    InventoryResponse importStock(InventoryItemRequest request, String warehouseId);

    InventoryResponse exportStock(InventoryItemRequest request, String warehouseId);

    void transferStock(TransferStockRequest request);

    ReserveStockResponse reserveStock(String productColorId, int quantity, long orderId);

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

    InventoryResponse approveTransfer(String inventoryId, TransferStatus transferStatus);

    List<InventoryResponse> getAllInventories();

    InventoryResponse getInventoryById(Long inventoryId);

    List<InventoryResponse> getPendingTransfers(String warehouseId);

    ProductLocationResponse getProductByStoreId(String storeId);

    List<InventoryResponse> getPendingReservations(String storeId);

    ProductLocationResponse getProductLocationsByWarehouse(String productColorId, String storeId);

    ProductLocationResponse getAllProductLocations(String productColorId);

    boolean checkZoneCapacity(String zoneId, int additionalQty);
}
