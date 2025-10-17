package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryTransaction;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.entity.Warehouse;
import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.AuthClient;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.InventoryTransactionRepository;
import com.example.inventoryservice.repository.LocationItemRepository;
import com.example.inventoryservice.repository.WarehouseRepository;
import com.example.inventoryservice.repository.ZoneRepository;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final AuthClient authClient;

    @Override
    @Transactional
    public InventoryResponse upsertInventory(String productColorId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
        ProductColorResponse product = getProductColorById(productColorId);
        if (product == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        Zone zone = zoneRepository.findById(locationItem.getZone().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        Warehouse warehouse = warehouseRepository.findById(zone.getWarehouse().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        if (quantity < minQuantity || quantity > maxQuantity) {
            throw new AppException(ErrorCode.INVALID_QUANTITY_RANGE);
        }

        Integer currentZoneTotal = inventoryRepository.sumQuantityByZoneId(zone.getId());
        if (currentZoneTotal + quantity > zone.getQuantity()) {
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
        }

        Integer currentWarehouseTotal = inventoryRepository.sumQuantityByWarehouseId(warehouse.getId());
        if (currentWarehouseTotal == null) currentWarehouseTotal = 0;
        if (currentWarehouseTotal + quantity > warehouse.getCapacity()) {
            throw new AppException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
        }

        Inventory inventory = inventoryRepository.findByLocationItem_IdAndProductColorId(productColorId, locationItemId)
                .orElse(new Inventory());
        inventory.setProductColorId(productColorId);
        inventory.setLocationItem(locationItem);
        inventory.setQuantity(quantity);
        inventory.setMinQuantity(minQuantity);
        inventory.setMaxQuantity(maxQuantity);
        if (inventory.getId() == null) {
            inventory.setReservedQuantity(0);
        }
        inventory.setStatus(com.example.inventoryservice.enums.EnumStatus.ACTIVE);

        inventoryRepository.save(inventory);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(quantity)
                .dateLocal(LocalDateTime.now())
                .note("Upsert inventory for product " + productColorId)
                .type(EnumTypes.IN)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }


    @Override
    public List<InventoryResponse> getInventoryByProduct(String productColorId) {
        return inventoryRepository.findAllByProductColorId(productColorId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponse increaseStock(String productColorId, String locationItemId, int amount, String warehouseId ) {
        Inventory inventory = inventoryRepository.findByLocationItem_IdAndProductColorId(locationItemId, productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        int newQuantity = inventory.getQuantity() + amount;
        if (newQuantity > inventory.getMaxQuantity()) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_QUANTITY);
        }

        Zone zone = zoneRepository.findById(inventory.getLocationItem().getZone().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        Warehouse warehouse = warehouseRepository.findById(zone.getWarehouse().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        int currentWarehouseTotal = inventoryRepository.sumQuantityByWarehouseId(warehouse.getId());
        if (currentWarehouseTotal + amount > warehouse.getCapacity()) {
            throw new AppException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Increase stock for product " + productColorId)
                .type(EnumTypes.IN)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse decreaseStock(String productColorId, String locationItemId, int amount, String warehouseId) {
        Inventory inventory = inventoryRepository.findByLocationItem_IdAndProductColorId(productColorId, locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getQuantity() < amount) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        int reserved = inventory.getReservedQuantity();
        if (reserved >= amount) {
            inventory.setReservedQuantity(reserved - amount);
        } else if (reserved > 0) {
            inventory.setReservedQuantity(0);
        }

        int newQuantity = inventory.getQuantity() - amount;
        if (newQuantity < inventory.getMinQuantity()) {
            throw new AppException(ErrorCode.BELOW_MIN_QUANTITY);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        Warehouse warehouse = warehouseRepository.findById(inventory.getLocationItem().getZone().getWarehouse().getId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Decrease stock for product " + productColorId + " (Fulfillment)")
                .type(EnumTypes.OUT)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(String productColorId, int amount) {
        if (!hasSufficientGlobalStock(productColorId, amount)) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }

        Inventory inventory = inventoryRepository.findAllByProductColorId(productColorId).stream()
                .filter(i -> (i.getQuantity() - i.getReservedQuantity()) >= amount)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK));

        inventory.setReservedQuantity(inventory.getReservedQuantity() + amount);
        inventoryRepository.save(inventory);

        Warehouse warehouse = inventory.getLocationItem() != null ?
                inventory.getLocationItem().getZone().getWarehouse() : null;

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Stock reserved for order of product " + productColorId)
                .type(EnumTypes.RESERVE)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse releaseStock(String productColorId, int amount) {
        Inventory inventory = inventoryRepository.findAllByProductColorId(productColorId).stream()
                .filter(i -> i.getReservedQuantity() >= amount)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        inventory.setReservedQuantity(inventory.getReservedQuantity() - amount);
        inventoryRepository.save(inventory);

        Warehouse warehouse = inventory.getLocationItem() != null ?
                inventory.getLocationItem().getZone().getWarehouse() : null;

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Reserved stock released for order cancellation of product " + productColorId)
                .type(EnumTypes.RELEASE) // Loại RELEASE
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    public boolean hasSufficientStock(String productId, String locationItemId, int requiredQty) {
        return inventoryRepository.findByProductColorIdAndLocationItemId(productId, locationItemId)
                .map(inventory -> (inventory.getQuantity() - inventory.getReservedQuantity()) >= requiredQty)
                .orElse(false);
    }

    @Override
    public boolean hasSufficientGlobalStock(String productId, int requiredQty) {
        int totalPhysical = inventoryRepository.getTotalQuantityByProductColorId(productId);
        int totalReserved = inventoryRepository.getTotalReservedQuantityByProductColorId(productId);

        int availableStock = totalPhysical - totalReserved;
        return availableStock >= requiredQty;
    }

    @Override
    @Transactional
    public boolean checkZoneCapacity(String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getId());
        return zone.getQuantity() - currentTotal > 0;
    }

    @Override
    public List<InventoryTransactionResponse> getAllTransactions() {
        return transactionRepository.findAll()
                .stream()
                .filter(inventoryTransaction ->  inventoryTransaction.getQuantity() > 0)
                .map(this::mapToTransactionResponse)
                .toList();
    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll()
                .stream()
                .filter(inventory -> (inventory.getQuantity() - inventory.getReservedQuantity()) > 0)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public int getTotalStockByProductColorId(String productColorId) {
        return inventoryRepository.getTotalQuantityByProductColorId(productColorId);
    }

    @Override
    public int getTotalAvailableStockByProductColorId(String productColorId) {
        int totalPhysical = inventoryRepository.getTotalQuantityByProductColorId(productColorId);
        int totalReserved = inventoryRepository.getTotalReservedQuantityByProductColorId(productColorId);
        return totalPhysical - totalReserved;
    }

    @Override
    public InventoryResponse getInventoryById(String inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        return mapToResponse(inventory);
    }

    @Override
    public void transferInventory(String productColorId, String locationItemId, int quantity, String warehouse1_Id, String warehouse2_Id) {
        Warehouse warehouse1 = warehouseRepository.findByIdAndIsDeletedFalse(warehouse1_Id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Warehouse warehouse2 = warehouseRepository.findByIdAndIsDeletedFalse(warehouse2_Id)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        decreaseStock(productColorId, locationItemId, quantity, warehouse1_Id);

        increaseStock(productColorId, locationItemId, quantity, warehouse2_Id);
    }

    @Override
    @Transactional
    public List<InventoryResponse> getInventoryByZone(String zoneId) {
        List<Inventory> inventories = inventoryRepository.findAllByLocationItem_Zone_ZoneId(zoneId);
        return inventories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<InventoryTransactionResponse> getTransactionHistory(String productColorId, String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        String warehouseId = zone.getWarehouse().getId();
        List<InventoryTransaction> transactions = transactionRepository.findByProductColorIddAndWarehouseId(productColorId, warehouseId);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        int availableQuantity = inventory.getQuantity() - inventory.getReservedQuantity();

        return InventoryResponse.builder()
                .id(inventory.getId())
                .productColorId(inventory.getProductColorId())
                .locationItemId(inventory.getLocationItem() != null ? inventory.getLocationItem().getId() : null)
                .quantity(inventory.getQuantity())
                .reserved_quantity(inventory.getReservedQuantity())
                .available_quantity(availableQuantity)
                .min_quantity(inventory.getMinQuantity())
                .max_quantity(inventory.getMaxQuantity())
                .status(inventory.getStatus())
                .build();
    }

    private InventoryTransactionResponse mapToTransactionResponse(InventoryTransaction transaction) {
        return InventoryTransactionResponse.builder()
                .transactionId(transaction.getId())
                .quantity(transaction.getQuantity())
                .dateLocal(transaction.getDateLocal())
                .type(transaction.getType())
                .note(transaction.getNote())
                .productColorId(transaction.getProductColorId())
                .userId(transaction.getUserId())
                .warehouseId(transaction.getWarehouse() != null ? transaction.getWarehouse().getId() : null)
                .build();
    }

    private ProductResponse getProductById(String productId) {
        ApiResponse<ProductResponse> response = productClient.getProductById(productId);
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData();
    }

    private ProductColorResponse getProductColorById(String productColorId) {
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
        if (response == null || response.getData() == null) {
            return null;
        }
        return response.getData();
    }

    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String username = authentication.getName();
        ApiResponse<AuthResponse> authResponse = authClient.getUserByUsername(username);
        if (authResponse == null || authResponse.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }

        String accountId = authResponse.getData().getId();
        ApiResponse<UserResponse> userResponse = userClient.getUserByAccountId(accountId);
        if (userResponse == null || userResponse.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        return userResponse.getData().getId();
    }
}
