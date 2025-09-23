//package com.example.inventoryservice.service;
//
//import com.example.inventoryservice.entity.Inventory;
//import com.example.inventoryservice.entity.InventoryTransaction;
//import com.example.inventoryservice.entity.LocationItem;
//import com.example.inventoryservice.entity.Zone;
//import com.example.inventoryservice.entity.InventoryTransaction;
//import com.example.inventoryservice.enums.EnumTypes;
//import com.example.inventoryservice.enums.ErrorCode;
//import com.example.inventoryservice.exception.AppException;
//import com.example.inventoryservice.feign.ProductClient;
//import com.example.inventoryservice.repository.InventoryRepository;
//import com.example.inventoryservice.repository.LocationItemRepository;
//import com.example.inventoryservice.repository.ZoneRepository;
//import com.example.inventoryservice.repository.InventoryTransactionRepository;
//import com.example.inventoryservice.response.ApiResponse;
//import com.example.inventoryservice.response.InventoryResponse;
//import com.example.inventoryservice.response.ProductResponse;
//import com.example.inventoryservice.service.inteface.InventoryService;
//import jakarta.persistence.EnumType;
//import jakarta.transaction.Transactional;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDateTime;
//import java.util.List;
//import java.util.stream.Collectors;
//
//@Service
//@RequiredArgsConstructor
//public class InventoryServiceImpl implements InventoryService {
//
//    private final InventoryRepository inventoryRepository;
//    private final LocationItemRepository locationItemRepository;
//    private final ZoneRepository zoneRepository;
//    private final InventoryTransactionRepository transactionRepository;
//    private final ProductClient productClient;
//
//    @Override
//    @Transactional
//    public InventoryResponse upsertInventory(String productId, String locationItemId, int quantity, int minQuantity, int maxQuantity) {
//        // Kiểm tra product
//        ProductResponse product = getProductById(productId);
//        if (product == null) {
//            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
//        }
//
//        // Kiểm tra locationItem
//        LocationItem locationItem = locationItemRepository.findById(locationItemId)
//                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
//        Zone zone = zoneRepository.findById(locationItem.getZone().getZoneId())
//                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
//
//        // Kiểm tra min/max quantity
//        if (quantity < minQuantity || quantity > maxQuantity) {
//            throw new AppException(ErrorCode.INVALID_QUANTITY_RANGE);
//        }
//
//        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getZoneId());
//        if (currentTotal + quantity > zone.getQuantity()) {
//            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
//        }
//
//        // Tạo hoặc cập nhật inventory
//        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
//                .orElse(new Inventory());
//        inventory.setProductId(productId);
//        inventory.setLocationItem(locationItem);
//        inventory.setQuantity(quantity);
//        inventory.setMinQuantity(minQuantity);
//        inventory.setMaxQuantity(maxQuantity);
//
//        inventoryRepository.save(inventory);
//
//        // Ghi transaction
//        InventoryTransaction transaction = InventoryTransaction.builder()
//                .quantity(quantity)
//                .dateLocal(LocalDateTime.now())
//                .note("Upsert inventory for product " + productId)
//                .type(EnumTypes.IN)
//                .productId(productId)
//                .warehouse(inventory.getLocationItem().getZone().getWarehouse())
//                .build();
//
//        transactionRepository.save(transaction);
//
//        return mapToResponse(inventory);
//    }
//
//    @Override
//    public List<InventoryResponse> getInventoryByProduct(String productId) {
//        return inventoryRepository.findAllByProductId(productId).stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    @Override
//    @Transactional
//    public InventoryResponse increaseStock(String productId, String locationItemId, int amount) {
//        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
//                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
//
//        int newQuantity = inventory.getQuantity() + amount;
//        if (newQuantity > inventory.getMaxQuantity()) {
//            throw new AppException(ErrorCode.EXCEEDS_MAX_QUANTITY);
//        }
//
//        // Kiểm tra sức chứa Zone
//        Zone zone = zoneRepository.findById(inventory.getLocationItem().getZone().getZoneId())
//                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
//        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getZoneId());
//        if (currentTotal + amount > zone.getQuantity()) {
//            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
//        }
//
//        inventory.setQuantity(newQuantity);
//        inventoryRepository.save(inventory);
//
//
//
//        InventoryTransaction transaction = InventoryTransaction.builder()
//                .quantity(newQuantity)
//                .dateLocal(LocalDateTime.now())
//                .note("Upsert inventory for product " + productId)
//                .type(EnumTypes.IN)
//                .productId(productId)
//                .warehouse(inventory.getLocationItem().getZone().getWarehouse())
//                .build();
//
//        transactionRepository.save(transaction);
//
//        return mapToResponse(inventory);
//    }
//
//    @Override
//    @Transactional
//    public InventoryResponse decreaseStock(String productId, String locationItemId, int amount) {
//        Inventory inventory = inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
//                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
//
//        if (inventory.getQuantity() < amount) {
//            throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
//        }
//
//        inventory.setQuantity(inventory.getQuantity() - amount);
//        inventoryRepository.save(inventory);
//
//        // Ghi transaction
//        InventoryTransaction transaction = new InventoryTransaction();
//        transaction.setQuantity(amount);
//        transaction.setDateLocal(LocalDateTime.now());
//        transaction.setType(EnumTypes.OUT);
//        transaction.setNote("Decrease stock for product " + productId);
//        transaction.setProductId(productId);
//        transaction.setWarehouse(inventory.getLocationItem().getZone().getWarehouse());
//        transactionRepository.save(transaction);
//
//        return mapToResponse(inventory);
//    }
//
//    @Override
//    public boolean hasSufficientStock(String productId, String locationItemId, int requiredQty) {
//        return inventoryRepository.findByProductIdAndLocationItemId(productId, locationItemId)
//                .map(inventory -> inventory.getQuantity() >= requiredQty)
//                .orElse(false);
//    }
//
//    @Override
//    public boolean hasSufficientGlobalStock(String productId, int requiredQty) {
//        Integer total = inventoryRepository.sumQuantityByProductId(productId);
//        return total != null && total >= requiredQty;
//    }
//
//    @Transactional
//    public ApiResponse<Integer> checkZoneCapacity(String zoneId) {
//        Zone zone = zoneRepository.findById(zoneId)
//                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
//        int currentTotal = inventoryRepository.sumQuantityByLocationItem_Zone_ZoneId(zone.getZoneId());
//        int remaining = zone.getQuantity() - currentTotal;
//        return null;
//    }
//
//    @Transactional
//    public List<InventoryResponse> getInventoryByZone(String zoneId) {
//        List<Inventory> inventories = inventoryRepository.findAllByLocationItem_Zone_ZoneId(zoneId);
//        return inventories.stream()
//                .map(this::mapToResponse)
//                .collect(Collectors.toList());
//    }
//
//    private InventoryResponse mapToResponse(Inventory inventory) {
//        return InventoryResponse.builder()
//                .id(inventory.getId())
//                .productId(inventory.getProductId())
//                .locationItem(inventory.getLocationItem())
//                .quantity(inventory.getQuantity())
//                .min_quantity(inventory.getMinQuantity())
//                .max_quantity(inventory.getMaxQuantity())
//                .build();
//    }
//
//    private ProductResponse getProductById(String productId) {
//        ApiResponse<ProductResponse> response = productClient.getProductById(productId);
//        if (response == null || response.getData() == null) {
//            return null;
//        }
//        return response.getData();
//    }
//}