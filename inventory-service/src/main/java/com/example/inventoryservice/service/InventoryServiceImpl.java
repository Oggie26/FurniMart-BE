package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.LocationItemRepository;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LocationItemRepository locationItemRepository;

    @Override
    public InventoryResponse upsertInventory(String productId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        Inventory inventory = inventoryRepository
                .findByProductIdAndLocationItemId(productId, locationItemId)
                .orElse(new Inventory());

        inventory.setProductId(productId);
        inventory.setLocationItem(locationItem);
        inventory.setQuantity(quantity);
        inventory.setMinQuantity(minQuantity);
        inventory.setMaxQuantity(maxQuantity);

        Inventory saved = inventoryRepository.save(inventory);
        return mapToResponse(saved);
    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(String productId) {
        return inventoryRepository.findAllByProductId(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }


    @Override
    public InventoryResponse increaseStock(String productId, String locationItemId, int amount) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .orElseThrow(() -> new RuntimeException("Inventory not found"));

        inventory.setQuantity(inventory.getQuantity() + amount);
        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Override
    public InventoryResponse decreaseStock(String productId, String locationItemId, int amount) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getQuantity() < amount) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        inventory.setQuantity(inventory.getQuantity() - amount);
        return mapToResponse(inventoryRepository.save(inventory));
    }

    @Override
    public boolean hasSufficientStock(String productId, String locationItemId, int requiredQty) {
        return inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .map(inventory -> inventory.getQuantity() >= requiredQty)
                .orElse(false);
    }

    @Override
    public boolean hasSufficientGlobalStock(String productId, int requiredQty) {
        Integer total = inventoryRepository.sumQuantityByProductId(productId);
        return total != null && total >= requiredQty;
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .locationItem(inventory.getLocationItem())
                .quantity(inventory.getQuantity())
                .min_quantity(inventory.getMinQuantity())
                .max_quantity(inventory.getMaxQuantity())
                .build();
    }
}
