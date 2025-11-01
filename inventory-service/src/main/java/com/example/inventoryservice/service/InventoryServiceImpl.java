package com.example.inventoryservice.service;

import com.example.inventoryservice.controller.InventoryController;
import com.example.inventoryservice.entity.*;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.repository.*;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.InventoryItemResponse;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;

    // ----------------- CREATE / UPDATE -----------------

    @Override
    @Transactional
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {
        Warehouse warehouse = warehouseRepository.findById(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Inventory inventory = Inventory.builder()
                .employeeId(request.getEmployeeId())
                .type(request.getType())
                .purpose(request.getPurpose())
                .date(LocalDate.now())
                .note(request.getNote())
                .warehouse(warehouse)
                .build();

        inventoryRepository.save(inventory);
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryItemResponse addInventoryItem(InventoryItemRequest request) {
        Inventory inventory = inventoryRepository.findById(request.getInventoryId())
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        LocationItem locationItem = locationItemRepository.findById(request.getLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        InventoryItem item = InventoryItem.builder()
                .inventory(inventory)
                .locationItem(locationItem)
                .productColorId(request.getProductColorId())
                .quantity(request.getQuantity())
                .reservedQuantity(0)
                .build();

        inventoryItemRepository.save(item);
        return mapToInventoryItemResponse(item);
    }

    // ----------------- IMPORT / EXPORT -----------------

    @Override
    @Transactional
    public InventoryResponse importStock(InventoryItemRequest request) {
        Inventory inventory = createInventory(
                request.getWarehouseId(),
                EnumTypes.IMPORT,
                EnumPurpose.STOCK_IN,
                "Import stock"
        );

        createInventoryItem(inventory, request.getLocationItemId(), request.getProductColorId(), request.getQuantity(), 0);
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse exportStock(InventoryItemRequest request) {
        List<InventoryItem> items = inventoryItemRepository
                .findAllByProductColorIdAndInventory_Warehouse_Id(request.getProductColorId(), request.getWarehouseId());

        if (items.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int remaining = request.getQuantity();

        for (InventoryItem item : items) {
            int available = item.getQuantity() - item.getReservedQuantity();
            if (available <= 0) continue;

            int toExport = Math.min(available, remaining);
            item.setQuantity(item.getQuantity() - toExport);
            inventoryItemRepository.save(item);

            remaining -= toExport;
            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }

        Inventory inventory = createInventory(
                request.getWarehouseId(),
                EnumTypes.EXPORT,
                EnumPurpose.STOCK_OUT,
                "Export stock"
        );

        createInventoryItem(inventory, request.getLocationItemId(), request.getProductColorId(), -request.getQuantity(), 0);

        return mapToInventoryResponse(inventory);
    }


    // ----------------- TRANSFER -----------------

    @Override
    @Transactional
    public void transferStock(TransferStockRequest request) {
        String productColorId = request.getProductColorId();
        int quantity = request.getQuantity();

        // 1️⃣ Lấy warehouse, zone và locationItem
        Warehouse fromWarehouse = warehouseRepository.findById(request.getFromWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Warehouse toWarehouse = warehouseRepository.findById(request.getToWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Zone fromZone = zoneRepository.findById(request.getFromZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        Zone toZone = zoneRepository.findById(request.getToZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        LocationItem fromLocation = locationItemRepository.findById(request.getFromLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        LocationItem toLocation = locationItemRepository.findById(request.getToLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        // 2️⃣ Kiểm tra tồn kho tại fromLocation
        if (!hasSufficientStock(productColorId, fromLocation.getId(), quantity)) {
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }

        // 3️⃣ EXPORT (kho đi)
        Inventory exportInventory = createInventory(
                fromWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer OUT to " + toWarehouse.getWarehouseName() + " / Zone: " + toZone.getZoneName()
        );
        createInventoryItem(exportInventory, fromLocation.getId(), productColorId, -quantity, 0);

        // 4️⃣ IMPORT (kho đến)
        Inventory importInventory = createInventory(
                toWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer IN from " + fromWarehouse.getWarehouseName() + " / Zone: " + fromZone.getZoneName()
        );
        createInventoryItem(importInventory, fromLocation.getId(), productColorId, quantity, 0);
    }




    // ----------------- RESERVE / RELEASE -----------------

    @Override
    @Transactional
    public InventoryResponse reserveStock(String productColorId, int quantity) {
        List<InventoryItem> items = inventoryItemRepository.findAllByProductColorId(productColorId);

        if (items.isEmpty()) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }

        int remaining = quantity;

        for (InventoryItem item : items) {
            int available = item.getQuantity() - item.getReservedQuantity();
            if (available <= 0) continue;

            int toReserve = Math.min(available, remaining);
            item.setReservedQuantity(item.getReservedQuantity() + toReserve);
            inventoryItemRepository.save(item);

            remaining -= toReserve;
            if (remaining <= 0) break;
        }

        if (remaining > 0) {
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
        }

        Inventory inventory = items.getFirst().getInventory();
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse releaseReservedStock(String productColorId, int quantity) {
        List<InventoryItem> items = inventoryItemRepository.findAllByProductColorId(productColorId);
        if (items.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        int remaining = quantity;
        for (InventoryItem item : items) {
            int reserved = item.getReservedQuantity();
            if (reserved > 0) {
                int toRelease = Math.min(reserved, remaining);
                item.setReservedQuantity(reserved - toRelease);
                inventoryItemRepository.save(item);
                remaining -= toRelease;
                if (remaining <= 0) break;
            }
        }

        return mapToInventoryResponse(items.getFirst().getInventory());
    }

    // ----------------- CHECK STOCK -----------------

    @Override
    public boolean hasSufficientStock(String productColorId, String warehouseId, int requiredQty) {
        List<InventoryItem> items = inventoryItemRepository.findAllByProductColorIdAndInventory_Warehouse_Id(productColorId, warehouseId);
        int available = items.stream().mapToInt(i -> i.getQuantity() - i.getReservedQuantity()).sum();
        return available >= requiredQty;
    }

    @Override
    public boolean hasSufficientGlobalStock(String productColorId, int requiredQty) {
        int total = getAvailableStockByProductColorId(productColorId);
        return total >= requiredQty;
    }

    @Override
    public int getTotalStockByProductColorId(String productColorId) {
        return inventoryItemRepository.findAllByProductColorId(productColorId)
                .stream()
                .mapToInt(InventoryItem::getQuantity)
                .sum();
    }

    @Override
    public int getAvailableStockByProductColorId(String productColorId) {
        return inventoryItemRepository.findAllByProductColorId(productColorId)
                .stream()
                .mapToInt(i -> i.getQuantity() - i.getReservedQuantity())
                .sum();
    }

    // ----------------- GET LIST -----------------

    @Override
    public List<InventoryResponse> getInventoryByWarehouse(String warehouseId) {
        return inventoryRepository.findAllByWarehouse_Id(warehouseId)
                .stream().map(this::mapToInventoryResponse).collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getInventoryByZone(String zoneId) {
        return inventoryItemRepository.findAllByLocationItem_Zone_Id(zoneId)
                .stream()
                .map(InventoryItem::getInventory)
                .distinct()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemResponse> getInventoryItemsByProduct(String productColorId) {
        return inventoryItemRepository.findAllByProductColorId(productColorId)
                .stream().map(this::mapToInventoryItemResponse).collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemResponse> getTransactionHistory(String productColorId, String zoneId) {
        return inventoryItemRepository.findAllByLocationItem_Zone_Id(zoneId)
                .stream()
                .filter(i -> i.getProductColorId().equals(productColorId))
                .map(this::mapToInventoryItemResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemResponse> getAllInventoryItems() {
        return inventoryItemRepository.findAll()
                .stream().map(this::mapToInventoryItemResponse).collect(Collectors.toList());
    }

    @Override
    public List<InventoryResponse> getAllInventories() {
        return inventoryRepository.findAll()
                .stream().map(this::mapToInventoryResponse).collect(Collectors.toList());
    }

    @Override
    public InventoryResponse getInventoryById(Long inventoryId) {
        Inventory inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));
        return mapToInventoryResponse(inventory);
    }

    @Override
    public boolean checkZoneCapacity(String zoneId, int additionalQty) {
        int currentQty = inventoryItemRepository.findAllByLocationItem_Zone_Id(zoneId)
                .stream().mapToInt(InventoryItem::getQuantity).sum();
        int maxCapacity = 10000;
        return (currentQty + additionalQty) <= maxCapacity;
    }

    // ----------------- PRIVATE HELPERS -----------------

    private Inventory createInventory(String warehouseId, EnumTypes type, EnumPurpose purpose, String note) {
        Warehouse warehouse = warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Inventory inventory = Inventory.builder()
                .employeeId("system")
                .type(type)
                .purpose(purpose)
                .date(LocalDate.now())
                .note(note)
                .warehouse(warehouse)
                .build();

        return inventoryRepository.save(inventory);
    }

    private void createInventoryItem(Inventory inventory, String locationItemId, String productColorId, int quantity, int reservedQty) {
        LocationItem locationItem = null;
        if (locationItemId != null) {
            locationItem = locationItemRepository.findById(locationItemId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        }

        InventoryItem item = InventoryItem.builder()
                .inventory(inventory)
                .locationItem(locationItem)
                .productColorId(productColorId)
                .quantity(quantity)
                .reservedQuantity(reservedQty)
                .build();

        inventoryItemRepository.save(item);
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        return InventoryResponse.builder()
                .id(inventory.getId())
                .employeeId(inventory.getEmployeeId())
                .type(inventory.getType())
                .purpose(inventory.getPurpose())
                .date(inventory.getDate())
                .note(inventory.getNote())
                .warehouse(inventory.getWarehouse())
                .build();
    }

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .productColorId(item.getProductColorId())
                .inventory(item.getInventory())
                .locationItem(item.getLocationItem())
                .build();
    }
}
