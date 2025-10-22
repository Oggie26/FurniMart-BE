package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.*;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.AuthClient;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.repository.*;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;
    private final WarehouseRepository warehouseRepository;
    private final InventoryTransactionRepository transactionRepository;
    private final ProductClient productClient;
    private final UserClient userClient;
    private final AuthClient authClient;

    // ✅ UP-SERT logic fix toàn bộ
    @Override
    @Transactional
    public InventoryResponse upsertInventory(String productColorId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
        log.info("🧩 Upsert inventory: productColorId={}, locationItemId={}, qty={}, min={}, max={}",
                productColorId, locationItemId, quantity, minQuantity, maxQuantity);

        ProductColorResponse product = getProductColorById(productColorId);
        if (product == null) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        LocationItem locationItem = locationItemRepository.findById(locationItemId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        Zone zone = locationItem.getZone();
        if (zone == null) throw new AppException(ErrorCode.ZONE_NOT_FOUND);

        Warehouse warehouse = zone.getWarehouse();
        if (warehouse == null) throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);

        if (quantity < 0 || minQuantity < 0 || maxQuantity < 0)
            throw new AppException(ErrorCode.INVALID_QUANTITY_RANGE);

        // 🔹 Lấy tồn cũ trong zone & warehouse
        Integer currentZoneTotal = inventoryRepository.sumQuantityByZoneId(zone.getId());
        if (currentZoneTotal == null) currentZoneTotal = 0;

        Integer currentWarehouseTotal = inventoryRepository.sumQuantityByWarehouseId(warehouse.getId());
        if (currentWarehouseTotal == null) currentWarehouseTotal = 0;

        // 🔹 Kiểm tra tồn tại inventory cũ
        Inventory existing = inventoryRepository
                .findByLocationItem_IdAndProductColorId(locationItemId, productColorId)
                .orElse(null);

        int oldQuantity = existing != null ? existing.getQuantity() : 0;
        int newZoneTotal = currentZoneTotal - oldQuantity + quantity;
        int newWarehouseTotal = currentWarehouseTotal - oldQuantity + quantity;

        // 🔹 Kiểm tra sức chứa zone và warehouse
        if (newZoneTotal > zone.getQuantity())
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);

        if (newWarehouseTotal > warehouse.getCapacity())
            throw new AppException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);

        if (quantity < minQuantity || quantity > maxQuantity)
            throw new AppException(ErrorCode.INVALID_QUANTITY_RANGE);

        // 🔹 Cập nhật hoặc tạo mới
        Inventory inventory = existing != null ? existing : new Inventory();
        inventory.setProductColorId(productColorId);
        inventory.setLocationItem(locationItem);
        inventory.setQuantity(quantity);
        inventory.setMinQuantity(minQuantity);
        inventory.setMaxQuantity(maxQuantity);
        if (inventory.getId() == null) inventory.setReservedQuantity(0);
        inventory.setStatus(EnumStatus.ACTIVE);

        inventoryRepository.save(inventory);

        // 🔹 Ghi log transaction
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
    @Transactional
    public InventoryResponse increaseStock(String productColorId, String locationItemId, int amount, String warehouseId) {
        Inventory inventory = inventoryRepository.findByLocationItem_IdAndProductColorId(locationItemId, productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        int newQuantity = inventory.getQuantity() + amount;
        if (newQuantity > inventory.getMaxQuantity())
            throw new AppException(ErrorCode.EXCEEDS_MAX_QUANTITY);

        Zone zone = inventory.getLocationItem().getZone();
        Integer currentZoneTotal = inventoryRepository.sumQuantityByZoneId(zone.getId());
        if (currentZoneTotal == null) currentZoneTotal = 0;
        if (currentZoneTotal + amount > zone.getQuantity())
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);

        Warehouse warehouse = zone.getWarehouse();
        Integer currentWarehouseTotal = inventoryRepository.sumQuantityByWarehouseId(warehouse.getId());
        if (currentWarehouseTotal == null) currentWarehouseTotal = 0;
        if (currentWarehouseTotal + amount > warehouse.getCapacity())
            throw new AppException(ErrorCode.WAREHOUSE_CAPACITY_EXCEEDED);

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        transactionRepository.save(InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Increase stock for product " + productColorId)
                .type(EnumTypes.IN)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build());

        return mapToResponse(inventory);
    }


    @Override
    @Transactional
    public InventoryResponse decreaseStock(String productColorId, String locationItemId, int amount, String warehouseId) {
        Inventory inventory = inventoryRepository.findByLocationItem_IdAndProductColorId( locationItemId, productColorId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getQuantity() < amount)
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);

        int reserved = inventory.getReservedQuantity();
        inventory.setReservedQuantity(Math.max(reserved - amount, 0));

        int newQuantity = inventory.getQuantity() - amount;
        if (newQuantity < inventory.getMinQuantity())
            throw new AppException(ErrorCode.BELOW_MIN_QUANTITY);

        inventory.setQuantity(newQuantity);
        inventoryRepository.save(inventory);

        Warehouse warehouse = inventory.getLocationItem().getZone().getWarehouse();
        transactionRepository.save(InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Decrease stock for product " + productColorId)
                .type(EnumTypes.OUT)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build());

        return mapToResponse(inventory);
    }

    @Override
    public void transferInventory(String productColorId, String locationItemId, int quantity, String warehouse1_Id, String warehouse2_Id) {
        decreaseStock(productColorId, locationItemId, quantity, warehouse1_Id);
        increaseStock(productColorId, locationItemId, quantity, warehouse2_Id);
    }

    @Override
    @Transactional
    public InventoryResponse reserveStock(String productColorId, int amount) {
        if (!hasSufficientGlobalStock(productColorId, amount))
            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);

        Inventory inventory = inventoryRepository.findAllByProductColorId(productColorId).stream()
                .filter(i -> (i.getQuantity() - i.getReservedQuantity()) >= amount)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INSUFFICIENT_STOCK));

        inventory.setReservedQuantity(inventory.getReservedQuantity() + amount);
        inventoryRepository.save(inventory);

        Warehouse warehouse = inventory.getLocationItem().getZone().getWarehouse();

        transactionRepository.save(InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Reserve stock for product " + productColorId)
                .type(EnumTypes.RESERVE)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build());

        return mapToResponse(inventory);
    }

    // ✅ Giải phóng tồn
    @Override
    @Transactional
    public InventoryResponse releaseStock(String productColorId, int amount) {
        Inventory inventory = inventoryRepository.findAllByProductColorId(productColorId).stream()
                .filter(i -> i.getReservedQuantity() >= amount)
                .findFirst()
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        inventory.setReservedQuantity(inventory.getReservedQuantity() - amount);
        inventoryRepository.save(inventory);

        Warehouse warehouse = inventory.getLocationItem().getZone().getWarehouse();

        transactionRepository.save(InventoryTransaction.builder()
                .quantity(amount)
                .dateLocal(LocalDateTime.now())
                .note("Release reserved stock for product " + productColorId)
                .type(EnumTypes.RELEASE)
                .productColorId(productColorId)
                .userId(getUserId())
                .warehouse(warehouse)
                .build());

        return mapToResponse(inventory);
    }

    // ✅ Các hàm tiện ích khác
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
        return (totalPhysical - totalReserved) >= requiredQty;
    }

    @Override
    public List<InventoryResponse> getInventoryByProduct(String productColorId) {
        return inventoryRepository.findAllByProductColorId(productColorId).stream()
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public boolean checkZoneCapacity(String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        Integer currentTotal = inventoryRepository.sumQuantityByZoneId(zone.getId());
        if (currentTotal == null) currentTotal = 0;
        return zone.getQuantity() - currentTotal > 0;
    }

    @Override
    public List<InventoryResponse> getAllInventory() {
        return inventoryRepository.findAll().stream()
                .filter(i -> (i.getQuantity() - i.getReservedQuantity()) > 0)
                .map(this::mapToResponse)
                .toList();
    }

    @Override
    public List<InventoryTransactionResponse> getAllTransactions() {
        return transactionRepository.findAll().stream()
                .filter(t -> t.getQuantity() > 0)
                .map(this::mapToTransactionResponse)
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
    public List<InventoryTransactionResponse> getTransactionHistory(String productColorId, String zoneId) {
        Zone zone = zoneRepository.findById(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        String warehouseId = zone.getWarehouse().getId();
        List<InventoryTransaction> transactions = transactionRepository.findByProductColorIddAndWarehouseId(productColorId, warehouseId);
        return transactions.stream().map(this::mapToTransactionResponse).toList();
    }

    @Override @Transactional public List<InventoryResponse> getInventoryByZone(String zoneId) {
        List<Inventory> inventories = inventoryRepository.findAllByLocationItem_Zone_ZoneId(zoneId);
        return inventories.stream()
                .map(this::mapToResponse)
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

    // ✅ Feign helper
    private ProductColorResponse getProductColorById(String productColorId) {
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
        if (response == null || response.getData() == null) return null;
        return response.getData();
    }

    private String getUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated() || "anonymousUser".equals(auth.getPrincipal()))
            throw new AppException(ErrorCode.UNAUTHENTICATED);

        ApiResponse<AuthResponse> authRes = authClient.getUserByUsername(auth.getName());
        if (authRes == null || authRes.getData() == null)
            throw new AppException(ErrorCode.NOT_FOUND_USER);

        ApiResponse<UserResponse> userRes = userClient.getUserByAccountId(authRes.getData().getId());
        if (userRes == null || userRes.getData() == null)
            throw new AppException(ErrorCode.NOT_FOUND_USER);

        return userRes.getData().getId();
    }
}
