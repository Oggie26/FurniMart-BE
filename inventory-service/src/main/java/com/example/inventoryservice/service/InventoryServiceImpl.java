package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.*;
import com.example.inventoryservice.enums.*;
import com.example.inventoryservice.event.UpdateStatusOrderCreatedEvent;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.*;
import com.example.inventoryservice.repository.*;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.InventoryService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
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
    private final DeliveryClient deliveryClient;
    private final PDFService pdfService;
//    private final KafkaTemplate<String, UpdateStatusOrderCreatedEvent> kafkaTemplate;



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

        boolean isStockOut = request.getType() == EnumTypes.EXPORT && request.getPurpose() == EnumPurpose.STOCK_OUT;

        if (isStockOut && request.getOrderId() != null && request.getOrderId() > 0) {
            inventory.setOrderId(getOrder(request.getOrderId()).getId());
        }

        inventoryRepository.save(inventory);

        if (request.getItems() == null || request.getItems().isEmpty()) {
            return mapToInventoryResponse(inventory);
        }

        Inventory transferInventory = null;
        boolean isTransferOut = request.getType() == EnumTypes.EXPORT && request.getPurpose() == EnumPurpose.TRANSFER_OUT;

        if (isTransferOut && request.getToWarehouseId() != null) {
            Warehouse toWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getToWarehouseId())
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

            transferInventory = Inventory.builder()
                    .employeeId(getProfile())
                    .type(EnumTypes.TRANSFER)
                    .purpose(EnumPurpose.REQUEST)
                    .warehouse(toWarehouse)
                    .transferStatus(TransferStatus.PENDING)
                    .note("Nhận hàng chuyển từ kho " + warehouse.getWarehouseName() + " - Mã phiếu xuất: " + inventory.getCode())
                    .date(LocalDate.now())
                    .build();

            inventoryRepository.save(transferInventory);
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

//                case EXPORT -> {
//                    List<InventoryItem> itemsInStock = inventoryItemRepository
//                            .findAllByProductColorIdAndInventory_Warehouse_Id(itemReq.getProductColorId(), warehouse.getId());
//
//                    if (itemsInStock.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
//
//                    int remainingQtyToExport = itemReq.getQuantity();
//
//                    for (InventoryItem it : itemsInStock) {
//                        if (remainingQtyToExport <= 0) break;
//
//                        int currentQty = it.getQuantity();
//                        if (currentQty <= 0) continue;
//
//                        int toExport = Math.min(currentQty, remainingQtyToExport);
//
//
//                        it.setQuantity(it.getQuantity() - toExport);
//
//                        if (isStockOut) {
//                            int newReserved = Math.max(0, it.getReservedQuantity() - toExport);
//                            it.setReservedQuantity(newReserved);
//                        }
//
//                        inventoryItemRepository.save(it);
//
//                        createInventoryItem(
//                                inventory,
//                                it.getLocationItem().getId(),
//                                itemReq.getProductColorId(),
//                                -toExport
//                        );
//
//                        if (isTransferOut && transferInventory != null) {
//                            createInventoryItem(
//                                    transferInventory,
//                                    it.getLocationItem().getId(),
//                                    itemReq.getProductColorId(),
//                                    Math.abs(toExport)
//                            );
//                        }
//
//                        remainingQtyToExport -= toExport;
//                    }
//
//                    if (remainingQtyToExport > 0) throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
//                }

                case EXPORT -> {
                    // 1. SỬA QUERY: Ưu tiên lấy những item đang có Reserved > 0 ra trước
                    List<InventoryItem> itemsInStock = inventoryItemRepository
                            .findItemsForExport(itemReq.getProductColorId(), warehouse.getId());

                    if (itemsInStock.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

                    int remainingQtyToExport = itemReq.getQuantity();

                    for (InventoryItem it : itemsInStock) {
                        if (remainingQtyToExport <= 0) break;

                        int currentQty = it.getQuantity();
                        if (currentQty <= 0) continue;

                        int toExport = Math.min(currentQty, remainingQtyToExport);

                        // --- LOGIC XÓA REVERSE & TRỪ KHO ---

                        // 1. Giảm tồn kho vật lý (Luôn luôn)
                        it.setQuantity(it.getQuantity() - toExport);

                        // 2. Nếu là xuất bán (STOCK_OUT), giảm luôn cả ReservedQuantity (Xóa Reverse)
                        if (isStockOut) {
                            // Chỉ trừ reserved nếu nó > 0
                            if (it.getReservedQuantity() > 0) {
                                // Trừ đi lượng xuất, nhưng không được nhỏ hơn 0
                                int newReserved = Math.max(0, it.getReservedQuantity() - toExport);
                                it.setReservedQuantity(newReserved);
                            }
                        }
                        // ------------------------------------

                        inventoryItemRepository.save(it);

                        // Tạo lịch sử xuất kho (như cũ)
                        createInventoryItem(
                                inventory,
                                it.getLocationItem().getId(),
                                itemReq.getProductColorId(),
                                -toExport // Số âm thể hiện xuất
                        );

                        // Logic chuyển kho (như cũ)
                        if (isTransferOut && transferInventory != null) {
                            createInventoryItem(
                                    transferInventory,
                                    it.getLocationItem().getId(),
                                    itemReq.getProductColorId(),
                                    Math.abs(toExport)
                            );
                        }

                        remainingQtyToExport -= toExport;
                    }

                    if (remainingQtyToExport > 0) throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
                }

                case TRANSFER -> {
                    if (request.getToWarehouseId() == null) throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);

                    Warehouse toWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getToWarehouseId())
                            .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

                    inventory.setWarehouse(toWarehouse);
                    inventory.setTransferStatus(TransferStatus.PENDING);
                    inventory.setNote("Yêu cầu chuyển hàng về kho " + toWarehouse.getWarehouseName());
                    inventoryRepository.save(inventory);

                    createInventoryItem(
                            inventory,
                            itemReq.getLocationItemId(),
                            itemReq.getProductColorId(),
                            itemReq.getQuantity()
                    );
                }

                default -> throw new AppException(ErrorCode.INVALID_TYPE);
            }
        }

        if (isStockOut && request.getOrderId() != null && request.getOrderId() > 0) {
            try {
                orderClient.updateOrderStatus(request.getOrderId(), EnumProcessOrder.PACKAGED);

                var deliveryRes = deliveryClient.getDeliveryAsiByOrderId(request.getOrderId());
                if (deliveryRes != null && deliveryRes.getData() != null) {
                    deliveryClient.updateDelivertAsiStatus(deliveryRes.getData().getId(), "PREPARING");
                }
            } catch (Exception e) {
                log.error("Error updating order/delivery status: {}", e.getMessage());
            }
        }

//        if (request.getType() == EnumTypes.EXPORT || request.getType() == EnumTypes.TRANSFER) {
//            try {
//                Inventory finalInventory = inventoryRepository.findByIdWithItems(inventory.getId())
//                        .orElse(inventory);
//
//                String pdfUrl = pdfService.generateExportPDF(finalInventory);
//                inventory.setPdfUrl(pdfUrl);
//                inventoryRepository.save(inventory);
//
//            } catch (Exception e) {
//                log.error("Error generating PDF: {}", e.getMessage());
//            }
//        }

        return mapToInventoryResponse(inventory);
    }


    @Override
    @Transactional
    public InventoryResponse approveTransfer(String inventoryId, TransferStatus transferStatus) {
        Inventory transfer = inventoryRepository.findById(Long.valueOf(inventoryId))
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (transfer.getPurpose() != EnumPurpose.REQUEST || transfer.getType() != EnumTypes.TRANSFER) {
            throw new AppException(ErrorCode.INVALID_TYPE);
        }

        if (transferStatus.equals(TransferStatus.REJECTED)) {
            transfer.setTransferStatus(TransferStatus.REJECTED);
            inventoryRepository.save(transfer);
            return mapToInventoryResponse(transfer);
        }
        if(transferStatus.equals(TransferStatus.ACCEPTED)){
            transfer.setTransferStatus(TransferStatus.ACCEPTED);
            inventoryRepository.save(transfer);
            return mapToInventoryResponse(transfer);
        }
        if(transferStatus.equals(TransferStatus.FINISHED)){
            transfer.setTransferStatus(TransferStatus.FINISHED);
            inventoryRepository.save(transfer);
            return mapToInventoryResponse(transfer);
        }
        return null;

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

        Inventory exportInventory = createInventory(
                fromWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer OUT to " + toWarehouse.getWarehouseName() + " / Zone: " + toZone.getZoneName()
        );

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
                    .storeId(warehouse.getStoreId())
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

//    @Override
//    @Transactional
//    public InventoryResponse reserveStock(String productColorId, int quantity, long orderId) {
//        // Lấy thông tin order
//        OrderResponse orderInfo = getOrder(orderId);
//
//        // Lấy kho của store
//        Warehouse warehouse = warehouseRepository
//                .findByStoreIdAndIsDeletedFalse(orderInfo.getStoreId())
//                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
//
//        // Lấy tất cả item cùng productColorId trong kho
//        List<InventoryItem> items = inventoryItemRepository
//                .findFullByProductColorIdAndWarehouseId(productColorId, warehouse.getId());
//        if (items.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
//
//        int remaining = quantity;
//
//        // Map lưu lượng reserve thực tế để rollback khi cần
//        Map<InventoryItem, Integer> reservedMap = new LinkedHashMap<>();
//
//        for (InventoryItem item : items) {
//            int available = item.getQuantity() - item.getReservedQuantity();
//            if (available <= 0) continue;
//
//            int toReserve = Math.min(available, remaining);
//            item.setReservedQuantity(item.getReservedQuantity() + toReserve);
//            inventoryItemRepository.save(item);
//
//            reservedMap.put(item, toReserve);
//            remaining -= toReserve;
//            if (remaining <= 0) break;
//        }
//
//        // Nếu không đủ stock → rollback
//        if (remaining > 0) {
//            for (Map.Entry<InventoryItem, Integer> entry : reservedMap.entrySet()) {
//                InventoryItem ri = entry.getKey();
//                int reservedQty = entry.getValue();
//                ri.setReservedQuantity(ri.getReservedQuantity() - reservedQty);
//                inventoryItemRepository.save(ri);
//            }
//            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
//        }
//
//        return mapToInventoryResponse(items.get(0).getInventory());
//    }
@Override
@Transactional
public ReserveStockResponse reserveStock(String productColorId, int quantity, long orderId) {
    // 1. Lấy thông tin Order và Warehouse
    OrderResponse orderInfo = getOrder(orderId);
    Warehouse warehouse = warehouseRepository
            .findByStoreIdAndIsDeletedFalse(orderInfo.getStoreId())
            .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

    // 2. Tìm các item vật lý trong kho
    List<InventoryItem> items = inventoryItemRepository
            .findFullByProductColorIdAndWarehouseId(productColorId, warehouse.getId());

    // 3. Tính toán khả dụng (Logic Best Effort)
    int totalAvailable = items.stream()
            .mapToInt(i -> i.getQuantity() - i.getReservedQuantity())
            .sum();

    int actualReserve = Math.min(quantity, totalAvailable);
    int missingQty = quantity - actualReserve;
    int remainingToReserve = actualReserve;

    // Map này để lưu lại dấu vết: Vừa lấy ở kệ nào, bao nhiêu cái? -> Để lưu vào phiếu
    Map<LocationItem, Integer> reservationLog = new HashMap<>();

    // 4. THỰC HIỆN GIỮ HÀNG TRÊN KỆ VẬT LÝ
    if (actualReserve > 0) {
        for (InventoryItem item : items) {
            int availableInItem = item.getQuantity() - item.getReservedQuantity();
            if (availableInItem <= 0) continue;

            int toReserve = Math.min(availableInItem, remainingToReserve);

            // a. Update số giữ chỗ vật lý (QUAN TRỌNG)
            item.setReservedQuantity(item.getReservedQuantity() + toReserve);
            inventoryItemRepository.save(item);

            // b. Lưu vào log để tí tạo phiếu
            reservationLog.put(item.getLocationItem(), toReserve);

            remainingToReserve -= toReserve;
            if (remainingToReserve <= 0) break;
        }

        Inventory reservationTicket = Inventory.builder()
                .employeeId("SYSTEM_AUTO")
                .type(EnumTypes.RESERVE)
                .purpose(EnumPurpose.REVERSE)
                .date(LocalDate.now())
                .warehouse(warehouse)
                .orderId(orderId)
                .code("RES-" + orderId + "-" + productColorId)
                .note("Giữ hàng tự động: " + actualReserve + "/" + quantity + " sản phẩm.")
                .build();
        inventoryRepository.save(reservationTicket);

        // 6. TẠO CHI TIẾT PHIẾU (Detail) -> Để Manager biết hàng nằm ở kệ nào
        for (Map.Entry<LocationItem, Integer> entry : reservationLog.entrySet()) {
            InventoryItem ticketDetail = InventoryItem.builder()
                    .inventory(reservationTicket) // Gắn vào phiếu giữ
                    .locationItem(entry.getKey()) // Gắn link tới kệ hàng thực tế
                    .productColorId(productColorId)
                    .quantity(entry.getValue())   // Số lượng giữ tại kệ này
                    .reservedQuantity(0)          // Trong phiếu lịch sử thì reserved = 0
                    .build();
            inventoryItemRepository.save(ticketDetail);
        }

        return ReserveStockResponse.builder()
                .inventory(reservationTicket)
                .quantityReserved(actualReserve)
                .quantityMissing(missingQty)
                .build();
    } else {
        // Trường hợp kho không có cái nào
        return ReserveStockResponse.builder()
                .inventory(null)
                .quantityReserved(0)
                .quantityMissing(quantity)
                .build();
    }
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
    public List<InventoryResponse> getPendingTransfers(String warehouseId) {
        return inventoryRepository
                .findAllByWarehouse_IdAndPurpose(
                        warehouseId,
                        EnumPurpose.REQUEST
                )
                .stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductLocationResponse getProductByStoreId(String storeId) {

        List<InventoryItem> items = inventoryItemRepository.findAllByStore(storeId);

        Map<String, ProductLocationResponse.LocationInfo> grouped = new HashMap<>();

        for (InventoryItem it : items) {

            int available = it.getQuantity() - it.getReservedQuantity();
            if (available <= 0) continue;

            LocationItem li = it.getLocationItem();
            Zone zone = li.getZone();
            Warehouse w = zone.getWarehouse();

            grouped.computeIfAbsent(li.getId(), k ->
                    ProductLocationResponse.LocationInfo.builder()
                            .warehouseId(w.getId())
                            .warehouseName(w.getWarehouseName())
                            .zoneId(zone.getId())
                            .zoneName(zone.getZoneName())
                            .locationItemId(li.getId())
                            .locationCode(li.getCode())
                            .totalQuantity(0)
                            .reserved(0)
                            .build());

            var info = grouped.get(li.getId());
            info.setTotalQuantity(info.getTotalQuantity() + it.getQuantity());
            info.setReserved(info.getReserved() + it.getReservedQuantity());
        }

        return ProductLocationResponse.builder()
                .storeId(storeId)
                .productColorId(null)
                .locations(new ArrayList<>(grouped.values()))
                .build();
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
        if (inventory == null) {
            throw new AppException(ErrorCode.INVENTORY_NOT_FOUND);
        }

        LocationItem locationItem = null;

        if (locationItemId != null && !locationItemId.isBlank()) {
            locationItem = locationItemRepository.findByIdAndIsDeletedFalse(locationItemId)
                    .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));
        } else if (inventory.getType() != EnumTypes.TRANSFER) {
            throw new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND);
        }

        InventoryItem.InventoryItemBuilder builder = InventoryItem.builder()
                .inventory(inventory)
                .productColorId(productColorId)
                .quantity(quantity)
                .reservedQuantity(0);

        if (locationItem != null) {
            builder.locationItem(locationItem);
        }

        InventoryItem item = builder.build();
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
                .pdfUrl(inventory.getPdfUrl())
                .note(inventory.getNote())
                .transferStatus(inventory.getTransferStatus())
                .warehouseId(inventory.getWarehouse().getId())
                .warehouseName(inventory.getWarehouse().getWarehouseName())
                .itemResponseList(itemResponseList)
                .build();
    }

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .quantity(Math.abs(item.getQuantity()))
                .reservedQuantity(item.getReservedQuantity())
                .productColorId(item.getProductColorId())
                .productName(getProductName(item.getProductColorId()).getProduct().getName())
                // locationId chỉ lấy nếu locationItem khác null
                .locationId(item.getLocationItem() != null ? item.getLocationItem().getId() : null)
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
        if (orderId == null || orderId <= 0) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        ApiResponse<OrderResponse> response = orderClient.getOderById(orderId);

        if (response == null || response.getData() == null) {
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
