package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.*;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumTypes;
import com.example.inventoryservice.enums.ErrorCode;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.AuthClient;
import com.example.inventoryservice.feign.OrderClient;
import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.feign.UserClient;
import com.example.inventoryservice.repository.*;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.*;
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
    private final AuthClient authClient;
    private final UserClient userClient;
    private final OrderClient orderClient;
    private final ProductClient productClient;


    @Override
    @Transactional
    public InventoryResponse createOrUpdateInventory(InventoryRequest request) {

        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Inventory inventory = Inventory.builder()
                .employeeId(getProfile())
                .type(request.getType())
                .purpose(request.getPurpose())
                .date(LocalDate.now())
                .note(request.getNote())
                .warehouse(warehouse)
                .build();

        if(request.getType() == EnumTypes.EXPORT){
            inventory.setOrderId(getOrder(request.getOrderId()).getId());
        }

        inventoryRepository.save(inventory);
        log.info("🔍 warehouseId nhận được: {}", request.getWarehouseId());

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return mapToInventoryResponse(inventory);
        }


        for (InventoryItemRequest itemReq : request.getItems()) {
            switch (request.getType()) {

                case IMPORT -> {
                    LocationItem location = locationItemRepository.findByIdAndIsDeletedFalse(itemReq.getLocationItemId())
                            .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));


                    int actualStock = inventoryItemRepository.getActualStock(location.getId());

                    if (actualStock + itemReq.getQuantity() > location.getQuantity()) {
                        throw new AppException(ErrorCode.LOCATION_CAPACITY_EXCEEDED);
                    }

                    createInventoryItem(
                            inventory,
                            itemReq.getLocationItemId(),
                            itemReq.getProductColorId(),
                            itemReq.getQuantity()
                    );
                }



                case EXPORT -> {
                    List<InventoryItem> items = inventoryItemRepository
                            .findAllByProductColorIdAndInventory_Warehouse_Id(itemReq.getProductColorId(),
                                    warehouse.getId());

                    if (items.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

                    int remaining = itemReq.getQuantity();

                    for (InventoryItem it : items) {
                        int available = it.getQuantity() - it.getReservedQuantity();
                        if (available <= 0) continue;

                        int toExport = Math.min(available, remaining);
                        it.setQuantity(it.getQuantity() - toExport);
                        inventoryItemRepository.save(it);

                        remaining -= toExport;
                        if (remaining <= 0) break;
                    }

                    if (remaining > 0)
                        throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

                    createInventoryItem(inventory, itemReq.getLocationItemId(),
                            itemReq.getProductColorId(), -itemReq.getQuantity());
                }

                case TRANSFER -> {
                    break;
                }


                default -> throw new AppException(ErrorCode.INVALID_TYPE);
            }
        }

        return mapToInventoryResponse(inventory);
    }


    @Override
    @Transactional
    public InventoryItemResponse addInventoryItem(InventoryItemRequest request, Long inventoryId) {
        if (inventoryId == null)
            throw new AppException(ErrorCode.INVENTORY_NOT_FOUND);

        Inventory inventory = inventoryRepository.findById(inventoryId)
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
    public InventoryResponse importStock(InventoryItemRequest request, String warehouseId) {

        LocationItem location = locationItemRepository.findByIdAndIsDeletedFalse(request.getLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        String zoneId = location.getZone().getId();
        if (!checkZoneCapacity(zoneId, request.getQuantity())) {
            throw new AppException(ErrorCode.ZONE_CAPACITY_EXCEEDED);
        }

        Inventory inventory = createInventory(
                warehouseId,
                EnumTypes.IMPORT,
                EnumPurpose.STOCK_IN,
                "Import stock"
        );

        createInventoryItem(inventory, request.getLocationItemId(), request.getProductColorId(), request.getQuantity());
        return mapToInventoryResponse(inventory);
    }


    @Override
    @Transactional
    public InventoryResponse exportStock(InventoryItemRequest request, String warehouseId) {

        List<InventoryItem> items = inventoryItemRepository
                .findAllByProductColorIdAndInventory_Warehouse_Id(request.getProductColorId(), warehouseId);

        if (items.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

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

        if (remaining > 0)
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

        Inventory inventory = createInventory(
                warehouseId,
                EnumTypes.EXPORT,
                EnumPurpose.STOCK_OUT,
                "Export stock"
        );

        return mapToInventoryResponse(inventory);
    }

    // ----------------- TRANSFER -----------------

    @Override
    @Transactional
    public void transferStock(TransferStockRequest request) {

        // Kiểm tra các thông tin chung
        if (request.getFromWarehouseId() == null || request.getToWarehouseId() == null)
            throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);
        if (request.getFromZoneId() == null || request.getToZoneId() == null)
            throw new AppException(ErrorCode.ZONE_NOT_FOUND);
        if (request.getFromLocationItemId() == null || request.getToLocationItemId() == null)
            throw new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND);

        Warehouse fromWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getFromWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
        Warehouse toWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getToWarehouseId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Zone fromZone = zoneRepository.findByIdAndIsDeletedFalse(request.getFromZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));
        Zone toZone = zoneRepository.findByIdAndIsDeletedFalse(request.getToZoneId())
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        LocationItem fromLocation = locationItemRepository.findByIdAndIsDeletedFalse(request.getFromLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        LocationItem toLocation = locationItemRepository.findByIdAndIsDeletedFalse(request.getToLocationItemId())
                .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

        // Tạo 1 phiếu xuất kho tổng
        Inventory exportInventory = createInventory(
                fromWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer OUT to " + toWarehouse.getWarehouseName() + " / Zone: " + toZone.getZoneName()
        );

        // Tạo 1 phiếu nhập kho tổng
        Inventory importInventory = createInventory(
                toWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer IN from " + fromWarehouse.getWarehouseName() + " / Zone: " + fromZone.getZoneName()
        );

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InventoryItemRequest item : request.getItems()) {
                if (!hasSufficientStock(item.getProductColorId(), fromWarehouse.getId(), item.getQuantity()))
                    throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

                createInventoryItem(exportInventory, fromLocation.getId(), item.getProductColorId(), -item.getQuantity());
                createInventoryItem(importInventory, toLocation.getId(), item.getProductColorId(), item.getQuantity());
            }
        } else {
            if (!hasSufficientStock(request.getProductColorId(), fromWarehouse.getId(), request.getQuantity()))
                throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

            createInventoryItem(exportInventory, fromLocation.getId(), request.getProductColorId(), -request.getQuantity());
            createInventoryItem(importInventory, toLocation.getId(), request.getProductColorId(), request.getQuantity());
        }
    }

    @Override
    public ProductLocationResponse getAllProductLocations(String productColorId) {
        List<InventoryItem> items = inventoryItemRepository.findFullByProductColorId(productColorId);

        Map<String, ProductLocationResponse.LocationInfo> grouped = new LinkedHashMap<>();

        for (InventoryItem item : items) {
            LocationItem li = item.getLocationItem();
            Zone zone = li.getZone();
            Warehouse warehouse = zone.getWarehouse();

            String key = li.getId();

            grouped.computeIfAbsent(key, k -> ProductLocationResponse.LocationInfo.builder()
                    .warehouseId(warehouse.getId())
                    .warehouseName(warehouse.getWarehouseName())
                    .zoneId(zone.getId())
                    .zoneName(zone.getZoneName())
                    .locationItemId(li.getId())
                    .locationCode(li.getCode())
                    .totalQuantity(0)
                    .reserved(0)
                    .build());

            ProductLocationResponse.LocationInfo info = grouped.get(key);
            info.setTotalQuantity(info.getTotalQuantity() + item.getQuantity());
            info.setReserved(info.getReserved() + item.getReservedQuantity());
        }

        return ProductLocationResponse.builder()
                .productColorId(productColorId)
                .locations(new ArrayList<>(grouped.values()))
                .build();
    }


    // ----------------- RESERVE / RELEASE -----------------

    @Override
    @Transactional
    public InventoryResponse reserveStock(String productColorId, int quantity) {
        List<InventoryItem> items = inventoryItemRepository.findAllByProductColorId(productColorId);
        if (items.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

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

        if (remaining > 0)
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

        return mapToInventoryResponse(items.get(0).getInventory());
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

        return mapToInventoryResponse(items.get(0).getInventory());
    }

    // ----------------- CHECK STOCK -----------------

    @Override
    public boolean hasSufficientStock(String productColorId, String warehouseId, int requiredQty) {
        List<InventoryItem> items =
                inventoryItemRepository.findAllByProductColorIdAndInventory_Warehouse_Id(productColorId, warehouseId);
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
                .stream().mapToInt(InventoryItem::getQuantity).sum();
    }

    @Override
    public int getAvailableStockByProductColorId(String productColorId) {
        return inventoryItemRepository.findAllByProductColorId(productColorId)
                .stream().mapToInt(i -> i.getQuantity() - i.getReservedQuantity()).sum();
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
                .stream().map(InventoryItem::getInventory).distinct()
                .map(this::mapToInventoryResponse).collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemResponse> getInventoryItemsByProduct(String productColorId) {
        return inventoryItemRepository.findAllByProductColorId(productColorId)
                .stream().map(this::mapToInventoryItemResponse).collect(Collectors.toList());
    }

    @Override
    public List<InventoryItemResponse> getTransactionHistory(String productColorId, String zoneId) {
        return inventoryItemRepository.findAllByLocationItem_Zone_Id(zoneId)
                .stream().filter(i -> i.getProductColorId().equals(productColorId))
                .map(this::mapToInventoryItemResponse).collect(Collectors.toList());
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
    public ProductLocationResponse getProductLocationsByWarehouse(String productColorId, String storeId) {
        Warehouse storeWarehouse = warehouseRepository.findByStoreId(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        List<InventoryItem> items =
                inventoryItemRepository.findFullByProductColorIdAndWarehouseId(productColorId, storeWarehouse.getId());

        Map<String, ProductLocationResponse.LocationInfo> grouped = new LinkedHashMap<>();

        for (InventoryItem item : items) {
            LocationItem li = item.getLocationItem();
            Zone zone = li.getZone();
            Warehouse itemWarehouse = zone.getWarehouse();

            String key = li.getId();

            grouped.computeIfAbsent(key, k -> ProductLocationResponse.LocationInfo.builder()
                    .warehouseId(itemWarehouse.getId())
                    .warehouseName(itemWarehouse.getWarehouseName())
                    .zoneId(zone.getId())
                    .zoneName(zone.getZoneName())
                    .locationItemId(li.getId())
                    .locationCode(li.getCode())
                    .totalQuantity(0)
                    .reserved(0)
                    .build());

            ProductLocationResponse.LocationInfo info = grouped.get(key);
            info.setTotalQuantity(info.getTotalQuantity() + item.getQuantity());
            info.setReserved(info.getReserved() + item.getReservedQuantity());
        }

        return ProductLocationResponse.builder()
                .productColorId(productColorId)
                .storeId(storeId)
                .locations(new ArrayList<>(grouped.values()))
                .build();
    }

    @Override
    public boolean checkZoneCapacity(String zoneId, int additionalQty) {
        Zone zone = zoneRepository.findByIdAndIsDeletedFalse(zoneId)
                .orElseThrow(() -> new AppException(ErrorCode.ZONE_NOT_FOUND));

        int currentQty = inventoryItemRepository.findAllByLocationItem_Zone_Id(zoneId)
                .stream()
                .mapToInt(InventoryItem::getQuantity)
                .sum();

        int maxCapacity = zone.getQuantity();
        return (currentQty + additionalQty) <= maxCapacity;
    }


    // ----------------- PRIVATE HELPERS -----------------

    private Inventory createInventory(String warehouseId, EnumTypes type, EnumPurpose purpose, String note) {
        Warehouse warehouse = warehouseRepository.findByIdAndIsDeletedFalse(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        Inventory inventory = Inventory.builder()
                .employeeId(getUserId())
                .type(type)
                .purpose(purpose)
                .date(LocalDate.now())
                .note(note)
                .warehouse(warehouse)
                .build();

        return inventoryRepository.save(inventory);
    }

    private void createInventoryItem(Inventory inventory, String locationItemId, String productColorId, int quantity) {
        if (inventory == null)
            throw new AppException(ErrorCode.INVENTORY_NOT_FOUND);

        LocationItem locationItem = null;
        if (locationItemId != null) {
            locationItem = locationItemRepository.findByIdAndIsDeletedFalse(locationItemId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        }

        InventoryItem item = InventoryItem.builder()
                .inventory(inventory)
                .locationItem(locationItem)
                .productColorId(productColorId)
                .quantity(quantity)
                .reservedQuantity(0)
                .build();

        inventoryItemRepository.save(item);

        if (inventory.getInventoryItems() == null) {
            inventory.setInventoryItems(new ArrayList<>());
        }
        inventory.getInventoryItems().add(item);
    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
        List<InventoryItemResponse> itemResponseList = Optional.ofNullable(inventory.getInventoryItems())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapToInventoryItemResponse)
                .toList();

        return InventoryResponse.builder()
                .id(inventory.getId())
                .employeeId(inventory.getEmployeeId())
                .type(inventory.getType())
                .purpose(inventory.getPurpose())
                .date(inventory.getDate())
                .note(inventory.getNote())
                .warehouseId(inventory.getWarehouse().getId())
                .warehouseName(inventory.getWarehouse().getWarehouseName())
                .itemResponseList(itemResponseList)
                .build();
    }

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .quantity(item.getQuantity())
                .reservedQuantity(item.getReservedQuantity())
                .productColorId(item.getProductColorId())
                .productName(getProductName(item.getProductColorId()).getProduct().getName())
//                .locationItem(item.getLocationItem())
                .inventoryId(item.getInventory().getId())
                .build();
    }

    private ProductColorResponse getProductName(String productColorId){
        ApiResponse<ProductColorResponse> response = productClient.getProductColor(productColorId);
        if(response.getData() == null || response == null){
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response.getData();
    }


    private String getUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHENTICATED);
        }

        String username = authentication.getName();
        ApiResponse<AuthResponse> response = authClient.getUserByUsername(username);

        if (response == null || response.getData() == null)
            throw new AppException(ErrorCode.NOT_FOUND_USER);

        ApiResponse<UserResponse> userId = userClient.getEmployeeByAccountId(response.getData().getId());
        if (userId == null || userId.getData() == null)
            throw new AppException(ErrorCode.NOT_FOUND_USER);

        return userId.getData().getId();
    }

    private OrderResponse getOrder(Long orderId) {
        ApiResponse<OrderResponse> response = orderClient.getOderById(orderId);
        if (response == null || response.getData() == null){
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }
        return response.getData();
    }

    private String getProfile(){
        ApiResponse<UserResponse> response = userClient.getEmployeeProfile();
        if(response == null || response.getData() == null){
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        return response.getData().getId();
    }

}
