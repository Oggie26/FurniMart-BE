package com.example.inventoryservice.service.inteface;

import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.PageResponse;

import java.util.List;

public interface IInventoryService {
    InventoryResponse createInventory(InventoryRequest inventoryRequest);
    InventoryResponse updateInventory(InventoryRequest inventoryRequest, String inventoryId);
    void deleteInventory(String inventoryId);
    List<InventoryResponse> getInventories();
    InventoryResponse getInventoryById(String inventoryId);
    InventoryResponse getByLocationAndProduct(String locationItemId, String productId);
    PageResponse<InventoryResponse> searchInventory(String request, int page, int size);
}
