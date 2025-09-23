package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.InventoryTransaction;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.entity.Zone;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.AuthClient;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.repository.LocationItemRepository;
import com.example.inventoryservice.repository.ZoneRepository;
import com.example.inventoryservice.repository.InventoryTransactionRepository;
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
import com.example.inventoryservice.response.*;
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final AuthClient authClient;

    @Override
    @Transactional
    public InventoryResponse upsertInventory(String productId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
        // Kiểm tra product
        ProductResponse product = getProductById(productId);
        if (product == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        // Kiểm tra locationItem
        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        Zone zone = zoneRepository.findById(locationItem.getZone().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        // Kiểm tra min/max quantity
        if (quantity < minQuantity || quantity > maxQuantity) {
            throw new AppException(ErrorCode.INVALID_QUANTITY_RANGE);
        }

        // Kiểm tra sức chứa Zone
        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getId());
        if (currentTotal + quantity > zone.getQuantity()) {
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED );
        }

        // Tạo hoặc cập nhật inventory
        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .orElse(new Inventory());
        inventory.setProductId(productId);
        inventory.setLocationItem(locationItem);
        inventory.setQuantity(quantity);
        inventory.setMinQuantity(minQuantity);
        inventory.setMaxQuantity(maxQuantity);
        inventory.setStatus(EnumStatus.ACTIVE);

        inventoryRepository.save(inventory);

        // Ghi transaction
        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(quantity)
                .dateLocal(LocalDateTime.now())
                .note("Upsert inventory for product " + productId)
                .type(EnumTypes.IN)
                .productId(productId)
                .userId(getUserId())
                .warehouse(locationItem.getZone().getWarehouse())
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(String productId) {
        return inventoryRepository.findAllByProductId(productId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public InventoryResponse increaseStock(String productId, String locationItemId, int amount) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        int newQuantity = inventory.getQuantity() + amount;
        if (newQuantity > inventory.getMaxQuantity()) {
            throw new AppException(ErrorCode.EXCEEDS_MAX_QUANTITY);
        }
        if (newQuantity < inventory.getMinQuantity()) {
            throw new AppException(ErrorCode.BELOW_MIN_QUANTITY);
        }

        Zone zone = zoneRepository.findById(inventory.getLocationItem().getZone().getId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getId());
        if (currentTotal + amount > zone.getQuantity()) {
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Increase stock for product " + productId)
                .type(EnumTypes.IN)
                .productId(productId)
                .userId(getUserId())
                .warehouse(inventory.getLocationItem().getZone().getWarehouse())
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse decreaseStock(String productId, String locationItemId, int amount) {
        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getQuantity() < amount) {
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
        }
        int newQuantity = inventory.getQuantity() - amount;
        if (newQuantity < inventory.getMinQuantity()) {
            throw new AppException(ErrorCode.BELOW_MIN_QUANTITY);
        }

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        InventoryTransaction transaction = InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Decrease stock for product " + productId)
                .type(EnumTypes.OUT)
                .productId(productId)
                .userId(getUserId())
                .warehouse(inventory.getLocationItem().getZone().getWarehouse())
                .build();
        transactionRepository.save(transaction);

        return mapToResponse(inventory);
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

    @Transactional
    public boolean checkZoneCapacity(String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getId());
        return zone.getQuantity() - currentTotal > 0;
    }

    @Transactional
    public List<InventoryResponse> getInventoryByZone(String zoneId) {
        List<Inventory> inventories = inventoryRepository.findAllByLocationItem_Zone_ZoneId(zoneId);
        return inventories.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public List<InventoryTransactionResponse> getTransactionHistory(String productId, String zoneId) {
        List<InventoryTransaction> transactions = transactionRepository.findByProductIdAndWarehouseId(productId, zoneId);
        return transactions.stream()
                .map(this::mapToTransactionResponse)
                .collect(Collectors.toList());
    }

    private InventoryResponse mapToResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .productId(inventory.getProductId())
                .locationItem(inventory.getLocationItem())
                .quantity(inventory.getQuantity())
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
                .userId(transaction.getUserId())
                .build();
    }

    private ProductResponse getProductById(String productId) {
        ApiResponse<ProductResponse> response = productClient.getProductById(productId);
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