package com.example.inventoryservice.service;

import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;

    @Override
    public InventoryResponse upsertInventory(String productId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
        return null;
    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(String productId) {
        return List.of();
    }

    @Override
    public InventoryResponse increaseStock(String inventoryId, int amount) {
        return null;
    }

    @Override
    public InventoryResponse decreaseStock(String inventoryId, int amount) {
        return null;
    }

    @Override
    public boolean hasSufficientStock(String productId, int requiredQty) {
        return false;
    }
}
