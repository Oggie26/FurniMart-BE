package com.example.inventoryservice.service;

import com.example.inventoryservice.entity.*;
import com.example.inventoryservice.enums.*;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.feign.*;
import com.example.inventoryservice.repository.*;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.inteface.InventoryService;
import org.springframework.transaction.annotation.Transactional;
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
    private final DeliveryClient deliveryClient;
    private final StoreClient storeClient;
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
                    List<InventoryItem> itemsInStock = inventoryItemRepository
                            .findItemsForExport(itemReq.getProductColorId(), warehouse.getId());

                    if (itemsInStock.isEmpty()) throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

                    int remainingQtyToExport = itemReq.getQuantity();

                    for (InventoryItem it : itemsInStock) {
                        if (remainingQtyToExport <= 0) break;

                        int currentQty = it.getQuantity();
                        if (currentQty <= 0) continue;

                        int toExport = Math.min(currentQty, remainingQtyToExport);


                        it.setQuantity(it.getQuantity() - toExport);

                        if (isStockOut) {
                            if (it.getReservedQuantity() > 0) {
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
        // 1. Lấy tất cả item liên quan đến sản phẩm này
        List<InventoryItem> items = inventoryItemRepository.findFullByProductColorId(productColorId);

        Map<String, ProductLocationResponse.LocationInfo> grouped = new LinkedHashMap<>();

        for (InventoryItem item : items) {
            if (item.getInventory() == null || item.getInventory().getType() == null) {
                continue;
            }

            if (EXCLUDED_TYPES.contains(item.getInventory().getType())) {
                continue;
            }

            if (item.getLocationItem() == null) {
                continue;
            }

            LocationItem li = item.getLocationItem();
            Zone zone = li.getZone();
            Warehouse warehouse = zone.getWarehouse();

            String key = li.getId(); // Group theo ID vị trí kệ

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

    @Override
    @Transactional
    public ReserveStockResponse reserveStock(String productColorId, int quantity, long orderId) {
        OrderResponse orderResponse = getOrder(orderId);

        Warehouse assignedWarehouse = warehouseRepository.findByStoreIdAndIsDeletedFalse(orderResponse.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));

        String productName = "Unknown Product";
        String colorName = "Unknown Color";
        try {
            var productInfo = productClient.getProductColor(productColorId);
            if (productInfo != null && productInfo.getData() != null) {
                productName = productInfo.getData().getProduct().getName();
                colorName = productInfo.getData().getColor().getColorName();
            }
        } catch (Exception e) {
            log.warn("⚠️ Không lấy được thông tin sản phẩm: {}", e.getMessage());
        }

        List<InventoryItem> allSystemItems = inventoryItemRepository
                .findByProductColorIdAndAvailableGreaterThanZero(productColorId);

        Map<Warehouse, List<InventoryItem>> warehouseMap = allSystemItems.stream()
                .collect(Collectors.groupingBy(item -> item.getLocationItem().getZone().getWarehouse()));

        int remainingToReserve = quantity;
        int totalReserved = 0;

        List<InventoryItem> itemsToUpdate = new ArrayList<>();
        List<Inventory> ticketsToCreate = new ArrayList<>();

        // =================================================================================
        // GIAI ĐOẠN 1: CASE 1 - QUÉT KHO ĐƯỢC ASSIGN TRƯỚC (Ưu tiên cao nhất)
        // =================================================================================
        if (warehouseMap.containsKey(assignedWarehouse)) {
            List<InventoryItem> homeItems = warehouseMap.get(assignedWarehouse);

            // Logic giữ hàng (tách hàm riêng để tái sử dụng)
            int reservedAtHome = reserveAtSpecificWarehouse(
                    assignedWarehouse, homeItems, remainingToReserve,
                    orderId, productColorId, TransferStatus.FINISHED,
                    itemsToUpdate, ticketsToCreate
            );

            remainingToReserve -= reservedAtHome;
            totalReserved += reservedAtHome;

            warehouseMap.remove(assignedWarehouse);
        }

        // =================================================================================
        // GIAI ĐOẠN 2: CASE 2 - NẾU THIẾU, QUÉT CÁC KHO KHÁC (Hàng xóm)
        // =================================================================================
        if (remainingToReserve > 0 && !warehouseMap.isEmpty()) {
            log.info("⚠️ Kho nhà không đủ, bắt đầu tìm kiếm tại các kho khác...");

            // Sắp xếp các kho còn lại theo tiêu chí:
            // 1. Kho nào đủ số lượng remaining ưu tiên trước (để gom 1 lần cho gọn)
            // 2. (Nâng cao) Kho nào gần Assigned Warehouse nhất (Cần tích hợp Google Maps/Distance Matrix)
            // Hiện tại: Sắp xếp theo tổng tồn kho giảm dần
            List<Warehouse> neighborWarehouses = warehouseMap.entrySet().stream()
                    .sorted((entry1, entry2) -> {
                        int qty1 = entry1.getValue().stream().mapToInt(i -> i.getQuantity() - i.getReservedQuantity()).sum();
                        int qty2 = entry2.getValue().stream().mapToInt(i -> i.getQuantity() - i.getReservedQuantity()).sum();
                        return Integer.compare(qty2, qty1); // Giảm dần
                    })
                    .map(Map.Entry::getKey)
                    .toList();

            for (Warehouse neighbor : neighborWarehouses) {
                if (remainingToReserve <= 0) break;

                // ĐÂY LÀ ĐIỂM MẤU CHỐT CỦA CASE 2:
                // Status là TO_TRANSFER (Cần chuyển hàng) để kho Assigned biết mà tạo lệnh ship về
                int reservedAtNeighbor = reserveAtSpecificWarehouse(
                        neighbor, warehouseMap.get(neighbor), remainingToReserve,
                        orderId, productColorId, TransferStatus.PENDING,
                        itemsToUpdate, ticketsToCreate
                );

                remainingToReserve -= reservedAtNeighbor;
                totalReserved += reservedAtNeighbor;
            }
        }

        // --- 3. Lưu xuống Database ---
        if (!itemsToUpdate.isEmpty()) {
            inventoryItemRepository.saveAll(itemsToUpdate); // Cập nhật reserved_quantity trên kệ
        }

        if (!ticketsToCreate.isEmpty()) {
            inventoryRepository.saveAll(ticketsToCreate); // Tạo phiếu log
        }

        // --- 4. Trả về kết quả ---
        ReserveStatus finalStatus;
        if (totalReserved == 0) {
            finalStatus = ReserveStatus.OUT_OF_STOCK;
        } else if (totalReserved < quantity) {
            finalStatus = ReserveStatus.PARTIAL_FULFILLMENT; // Thiếu hàng
        } else {
            finalStatus = ReserveStatus.FULL_FULFILLMENT; // Đủ hàng
        }

        return ReserveStockResponse.builder()
                .reservations(ticketsToCreate.stream().map(this::mapToInventoryResponse).toList())
                .quantityReserved(totalReserved)
                .quantityMissing(quantity - totalReserved)
                .reserveStatus(finalStatus)
                .build();
    }

    /**
     * Hàm hỗ trợ xử lý logic trừ kho và tạo phiếu cho 1 Warehouse cụ thể
     */
//    private int reserveAtSpecificWarehouse(
//            Warehouse warehouse,
//            List<InventoryItem> items,
//            int needQty,
//            long orderId,
//            String productColorId,
//            TransferStatus transferStatus,
//            List<InventoryItem> itemsToUpdateOut,
//            List<Inventory> ticketsToCreateOut
//    ) {
//        int actuallyReserved = 0;
//        List<InventoryItem> tempItemsToUpdate = new ArrayList<>();
//
//        for (InventoryItem item : items) {
//            if (needQty <= 0) break;
//
//            int available = item.getQuantity() - item.getReservedQuantity();
//            if (available <= 0) continue;
//
//            int toTake = Math.min(available, needQty);
//
//            item.setReservedQuantity(item.getReservedQuantity() + toTake);
//            tempItemsToUpdate.add(item);
//
//            needQty -= toTake;
//            actuallyReserved += toTake;
//        }
//
//        if (actuallyReserved > 0) {
//            itemsToUpdateOut.addAll(tempItemsToUpdate);
//            String storeName = "Unknown Store";
//            try {
//                ApiResponse<StoreResponse> storeResponse = storeClient.getStoreById(warehouse.getStoreId());
//                if (storeResponse != null && storeResponse.getData() != null) {
//                    storeName = storeResponse.getData().getName();
//                }
//            } catch (Exception e) {
//                // Log lỗi nếu cần, không để chết luồng
//            }
//
//            Inventory ticket = Inventory.builder()
//                    .employeeId("SYSTEM_AUTO")
//                    .type(EnumTypes.RESERVE)
//                    .purpose(EnumPurpose.RESERVE)
//                    .date(LocalDate.now())
//                    .warehouse(warehouse)
//                    .orderId(orderId)
//                    .code("RES_" + orderId + "_" + productColorId + "_" + warehouse.getId())
//                    .transferStatus(transferStatus)
//                    .note("Cửa hàng: " + storeName + " | Giữ hàng: " + actuallyReserved + " items. Trạng thái: " + transferStatus + "\n" +
//                            "Thiếu hàng: " + needQty)
//                    .build();
//
//            ticketsToCreateOut.add(ticket);
//        }
//
//        return actuallyReserved;
//    }
    private int reserveAtSpecificWarehouse(
            Warehouse warehouse,
            List<InventoryItem> items,
            int needQty,
            long orderId,
            String productColorId,
            TransferStatus transferStatus,
            List<InventoryItem> itemsToUpdateOut,
            List<Inventory> ticketsToCreateOut
    ) {
        int actuallyReserved = 0;
        List<InventoryItem> tempItemsToUpdate = new ArrayList<>();

        // 1. Logic trừ kho (Giữ nguyên)
        for (InventoryItem item : items) {
            if (needQty <= 0) break;

            int available = item.getQuantity() - item.getReservedQuantity();
            if (available <= 0) continue;

            int toTake = Math.min(available, needQty);

            item.setReservedQuantity(item.getReservedQuantity() + toTake);
            tempItemsToUpdate.add(item);

            needQty -= toTake;
            actuallyReserved += toTake;
        }

        if (actuallyReserved > 0) {
            itemsToUpdateOut.addAll(tempItemsToUpdate);

            String storeName = "Unknown Store";
            String storeId = (warehouse.getStoreId() != null) ? warehouse.getStoreId() : "N/A";

            try {
                if (warehouse.getStoreId() != null) {
                    ApiResponse<StoreResponse> response = storeClient.getStoreById(warehouse.getStoreId());
                    if (response != null && response.getData() != null) {
                        storeName = response.getData().getName();
                    }
                }
            } catch (Exception e) {
                log.warn("⚠️ Không lấy được tên store cho warehouse {}: {}", warehouse.getId(), e.getMessage());
            }

            int missingQty = needQty;

            Inventory ticket = Inventory.builder()
                    .employeeId("SYSTEM_AUTO")
                    .type(EnumTypes.RESERVE)
                    .purpose(EnumPurpose.RESERVE)
                    .date(LocalDate.now())
                    .warehouse(warehouse)
                    .orderId(orderId)
                    .code("RES_" + storeId + "_" + orderId + "_" + productColorId)
                    .transferStatus(transferStatus)
                    .note("Cửa hàng: " + storeName + " (" + storeId + ")\n" +
                            "Giữ hàng: " + actuallyReserved + " | Trạng thái: " + transferStatus + "\n" +
                            "Thiếu: " + missingQty)
                    .build();

            ticketsToCreateOut.add(ticket);
        }

        return actuallyReserved;
    }
    @Override
    @Transactional
    public ReserveStockResponse releaseReservedStock(String productColorId, int quantity, Long orderId) {
        List<Inventory> tickets = inventoryRepository.findAllByOrderId(orderId);
        List<Inventory> relevantTickets = tickets.stream()
                .filter(t -> t.getType() == EnumTypes.RESERVE)
                .filter(t -> t.getInventoryItems().stream()
                        .anyMatch(i -> i.getProductColorId().equals(productColorId)))
                .toList();

        if (relevantTickets.isEmpty()) {
            return null;
        }

        List<InventoryItem> stockToUpdate = new ArrayList<>();
        int quantityToRelease = quantity;
        int totalReleased = 0;
        Map<Warehouse, Integer> releasedPerWarehouse = new HashMap<>();

        for (Inventory ticket : relevantTickets) {
            if (quantityToRelease <= 0)
                break;

            for (InventoryItem ticketItem : ticket.getInventoryItems()) {
                if (!ticketItem.getProductColorId().equals(productColorId))
                    continue;

                int canRelease = ticketItem.getQuantity();
                int actualRelease = Math.min(canRelease, quantityToRelease);

                if (actualRelease <= 0)
                    continue;

                InventoryItem stockItem = inventoryItemRepository
                        .findByProductColorIdAndLocationItemId(productColorId, ticketItem.getLocationItem().getId())
                        .orElse(null);

                if (stockItem != null) {
                    stockItem.setReservedQuantity(Math.max(0, stockItem.getReservedQuantity() - actualRelease));
                    stockToUpdate.add(stockItem);
                    totalReleased += actualRelease;
                    quantityToRelease -= actualRelease;
                    releasedPerWarehouse.merge(ticket.getWarehouse(), actualRelease, Integer::sum);
                }

                if (quantityToRelease <= 0)
                    break;
            }
        }

        inventoryItemRepository.saveAll(stockToUpdate);

        List<Inventory> releaseTickets = new ArrayList<>();
        for (Map.Entry<Warehouse, Integer> entry : releasedPerWarehouse.entrySet()) {
            Inventory releaseTicket = Inventory.builder()
                    .employeeId("SYSTEM_AUTO")
                    .type(EnumTypes.RELEASE)
                    .purpose(EnumPurpose.RETURN)
                    .date(LocalDate.now())
                    .warehouse(entry.getKey())
                    .orderId(orderId)
                    .code("REL-" + orderId + "-" + productColorId + "-" + entry.getKey().getId() + "-"
                            + System.currentTimeMillis())
                    .note("Release Stock Split: " + entry.getValue())
                    .build();
            releaseTickets.add(releaseTicket);
        }
        inventoryRepository.saveAll(releaseTickets);

        return ReserveStockResponse.builder()
                .quantityReserved(0)
                .quantityMissing(0)
                .build();
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

    private static final List<EnumTypes> EXCLUDED_TYPES = List.of(
            EnumTypes.RESERVE,
            EnumTypes.EXPORT,
            EnumTypes.TRANSFER
    );

    private static final List<EnumTypes> VIRTUAL_STOCK_TYPES = List.of(
            EnumTypes.RESERVE,
            EnumTypes.EXPORT,
            EnumTypes.TRANSFER
    );

    @Override
    public int getTotalStockByProductColorId(String productColorId) {
        return Objects.requireNonNullElse(
                inventoryItemRepository.calculateTotalPhysicalStock(productColorId, VIRTUAL_STOCK_TYPES),
                0
        );
    }

    @Override
    public int getAvailableStockByProductColorId(String productColorId) {
        Integer rawStock = inventoryItemRepository.calculateRealAvailableStock(productColorId, VIRTUAL_STOCK_TYPES);
        return (rawStock == null) ? 0 : Math.max(0, rawStock);
    }


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
    public List<InventoryResponse> getPendingReservations(String storeId) {
        Warehouse warehouse = warehouseRepository.findByStoreIdAndIsDeletedFalse(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        List<Inventory> reservations = inventoryRepository.findPendingReservations(warehouse.getId());

        return reservations.stream()
                .map(this::mapToInventoryResponse)
                .collect(Collectors.toList());
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

    @Override
    @Transactional(readOnly = true)
    public List<LowStockAlertResponse> getLowStockProducts(Integer threshold) {
        int defaultThreshold = threshold != null ? threshold : 10;

        List<String> allProductColorIds = inventoryItemRepository.findDistinctProductColorIds();

        List<LowStockAlertResponse> alerts = new ArrayList<>();

        for (String pid : allProductColorIds) {
            try {
                // Các hàm tính toán này (getTotal/getAvailable) nên dùng query SUM thuần túy (native query hoặc JPQL select sum)
                // để tránh load entity.
                int available = getAvailableStockByProductColorId(pid);

                if (available <= defaultThreshold && available >= 0) {
                    ProductColorResponse pc = getProductName(pid);
                    int total = getTotalStockByProductColorId(pid);

                    alerts.add(LowStockAlertResponse.builder()
                            .productColorId(pid)
                            .productColor(pc)
                            .productName(pc.getProduct() != null ? pc.getProduct().getName() : "N/A")
                            .currentStock(available)
                            .totalStock(total)
                            .reservedStock(total - available)
                            .threshold(defaultThreshold)
                            .alertLevel(available == 0 ? "CRITICAL" : "LOW")
                            .message("Stock is low!")
                            .build());
                }
            } catch (Exception e) {
                log.warn("Error processing low stock for {}: {}", pid, e.getMessage());
            }
        }
        return alerts;
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

//    private InventoryResponse mapToInventoryResponse(Inventory inventory) {
//        List<InventoryItemResponse> itemResponseList = Optional.ofNullable(inventory.getInventoryItems())
//                .orElse(Collections.emptyList())
//                .stream()
//                .map(this::mapToInventoryItemResponse)
//                .toList();
//
//        return InventoryResponse.builder()
//                .id(inventory.getId())
//                .employeeId(inventory.getEmployeeId())
//                .type(inventory.getType())
//                .purpose(inventory.getPurpose())
//                .date(inventory.getDate())
//                .pdfUrl(inventory.getPdfUrl())
//                .note(inventory.getNote())
//                .orderId(inventory.getOrderId())
//                .transferStatus(inventory.getTransferStatus())
//                .warehouseId(inventory.getWarehouse().getId())
//                .warehouseName(inventory.getWarehouse().getWarehouseName())
//                .itemResponseList(itemResponseList)
//                .build();
//    }

    private InventoryResponse mapToInventoryResponse(Inventory inventory) { // <--- Chỉ còn 1 tham số

        List<InventoryItemResponse> itemResponseList = Optional.ofNullable(inventory.getInventoryItems())
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapToInventoryItemResponse)
                .collect(Collectors.toList());

        if (itemResponseList.isEmpty()) {
            int quantity = parseQuantityFromNote(inventory.getNote());

            String extractedProductId = extractProductIdFromCode(inventory.getCode());

            if (quantity > 0 && extractedProductId != null) {
                InventoryItemResponse virtualItem = InventoryItemResponse.builder()
                        .id(null)
                        .quantity(quantity)
                        .reservedQuantity(0)
                        .productColorId(extractedProductId)
                        .productName(extractedProductId)
                        .locationId(inventory.getWarehouse().getId())
                        .inventoryId(inventory.getId())
                        .build();

                itemResponseList = List.of(virtualItem);
            }
        }

        return InventoryResponse.builder()
                .id(inventory.getId())
                .employeeId(inventory.getEmployeeId())
                .type(inventory.getType())
                .purpose(inventory.getPurpose())
                .date(inventory.getDate())
                .pdfUrl(inventory.getPdfUrl())
                .note(inventory.getNote())
                .orderId(inventory.getOrderId())
                .transferStatus(inventory.getTransferStatus())
                .warehouseId(inventory.getWarehouse().getId())
                .warehouseName(inventory.getWarehouse().getWarehouseName())
                .itemResponseList(itemResponseList)
                .build();
    }

    // Hàm tách ProductID từ mã phiếu
    private String extractProductIdFromCode(String code) {
        try {
            if (code != null && code.startsWith("RES-")) {
                String[] parts = code.split("-");
                if (parts.length >= 3) {
                    return parts[2];
                }
            }
        } catch (Exception e) {}
        return null;
    }

    private int parseQuantityFromNote(String note) {
        try {
            if (note != null && note.contains("Reserved:")) {
                String[] parts = note.split(" ");
                for (String part : parts) {
                    if (part.matches("\\d+")) return Integer.parseInt(part);
                }
            }
        } catch (Exception e) {}
        return 0;
    }

    private InventoryItemResponse mapToInventoryItemResponse(InventoryItem item) {
        return InventoryItemResponse.builder()
                .id(item.getId())
                .quantity(Math.abs(item.getQuantity()))
                .reservedQuantity(item.getReservedQuantity())
                .productColorId(item.getProductColorId())
                .productName(getProductName(item.getProductColorId()).getProduct().getName())
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

    private StoreResponse getStore(String storeId) {
        try {
            return storeClient.getStoreById(storeId).getData();
        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_WAREHOUSE_STOREID);
        }
    }

}
