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
@SuppressWarnings("unused")
public class InventoryServiceImpl implements InventoryService {

    private final InventoryRepository inventoryRepository;
    private final InventoryItemRepository inventoryItemRepository;
    private final WarehouseRepository warehouseRepository;
    private final LocationItemRepository locationItemRepository;
    private final ZoneRepository zoneRepository;
    private final AuthClient authClient;
    private final UserClient userClient;
    private final OrderClient orderClient;
    private final ProductServiceClient productServiceClient;
    private final DeliveryClient deliveryClient;
    private final StoreClient storeClient;
    private final InventoryReservedWarehouseRepository reservedWarehouseRepository;
    private final PDFService pdfService;

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
        boolean isTransferOut = request.getType() == EnumTypes.EXPORT && request.getPurpose() == EnumPurpose.MOVE;

        if (isTransferOut && request.getToWarehouseId() != null) {
            Warehouse toWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getToWarehouseId())
                    .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

            transferInventory = Inventory.builder()
                    .employeeId(getProfile())
                    .type(EnumTypes.TRANSFER)
                    .purpose(EnumPurpose.REQUEST)
                    .warehouse(toWarehouse)
                    .transferStatus(TransferStatus.PENDING_CONFIRM)
                    .note("Nhận hàng chuyển từ kho " + warehouse.getWarehouseName() + " - Mã phiếu xuất: "
                            + inventory.getCode())
                    .toWarehouseId(warehouse.getId())
                    .toWarehouseName(warehouse.getWarehouseName())
                    .date(LocalDate.now())
                    .build();

            inventory.setToWarehouseId(toWarehouse.getId());
            inventory.setToWarehouseName(toWarehouse.getWarehouseName());
            inventoryRepository.save(transferInventory);
        }

        for (InventoryItemRequest itemReq : request.getItems()) {
            switch (request.getType()) {

                case IMPORT -> {
                    LocationItem location = locationItemRepository
                            .findByIdAndIsDeletedFalse(itemReq.getLocationItemId())
                            .orElseThrow(() -> new AppException(ErrorCode.LOCATIONITEM_NOT_FOUND));

                    int actualStock = inventoryItemRepository.getActualStock(location.getId());

                    if (actualStock + itemReq.getQuantity() > location.getQuantity()) {
                        throw new AppException(ErrorCode.LOCATION_CAPACITY_EXCEEDED);
                    }

                    createInventoryItem(
                            inventory,
                            itemReq.getLocationItemId(),
                            itemReq.getProductColorId(),
                            itemReq.getQuantity());
                }

                case EXPORT -> {

                    List<InventoryItem> itemsInStock = inventoryItemRepository
                            .findItemsForExport(itemReq.getProductColorId(), warehouse.getId());

                    if (itemsInStock.isEmpty())
                        throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

                    int remainingQty = itemReq.getQuantity();

                    // --- Validation Block ---
                    long currentStock = 0;
                    long availableForTransfer = 0;
                    for (InventoryItem i : itemsInStock) {
                        currentStock += i.getQuantity();
                        availableForTransfer += (i.getQuantity() - i.getReservedQuantity());
                    }

                    if (currentStock < remainingQty) {
                        throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
                    }
                    // Nếu là chuyển kho, không được lấy hàng Reserved
                    if (!isStockOut && availableForTransfer < remainingQty) {
                        throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);
                    }
                    // ------------------------

                    for (InventoryItem it : itemsInStock) {
                        if (remainingQty <= 0)
                            break;

                        int available = it.getQuantity();

                        // Logic kiểm tra available cho Transfer vs Sale
                        if (!isStockOut) {
                            available = it.getQuantity() - it.getReservedQuantity();
                        }

                        if (available <= 0)
                            continue;

                        int toExport = Math.min(available, remainingQty);

                        // 1. Trừ kho thực tế
                        it.setQuantity(it.getQuantity() - toExport);

                        // 2. Nếu là bán hàng, update Reserved
                        if (isStockOut && it.getReservedQuantity() > 0) {
                            int newReserved = Math.max(0, it.getReservedQuantity() - toExport);
                            it.setReservedQuantity(newReserved);
                        }
                        inventoryItemRepository.save(it);

                        // 3. Tạo record lịch sử (Số dương, nhưng không có Location -> Không tính vào
                        // tồn kho)
                        createInventoryItem(
                                inventory,
                                null, // Không gắn location để tránh bị tính vào query getActualStock
                                itemReq.getProductColorId(),
                                toExport); // Số dương đẻ hiển thị đẹp

                        remainingQty -= toExport;
                    }

                    if (remainingQty > 0)
                        throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

                    // -----------------------------
                    // CHUYỂN KHO → THÊM VÀO PHIẾU NHẬN (ĐÃ TẠO Ở TRÊN)
                    // -----------------------------
                    if (isTransferOut && transferInventory != null) {
                        createInventoryItem(
                                transferInventory,
                                null,
                                itemReq.getProductColorId(),
                                itemReq.getQuantity());
                    }
                }
                case TRANSFER -> {
                    if (request.getToWarehouseId() == null)
                        throw new AppException(ErrorCode.WAREHOUSE_NOT_FOUND);

                    Warehouse toWarehouse = warehouseRepository.findByIdAndIsDeletedFalse(request.getToWarehouseId())
                            .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

                    inventory.setWarehouse(toWarehouse);
                    inventory.setTransferStatus(TransferStatus.PENDING);
                    inventory.setNote("Yêu cầu chuyển hàng về kho " + toWarehouse.getWarehouseName());
                    inventory.setToWarehouseId(warehouse.getId());
                    inventory.setToWarehouseName(warehouse.getWarehouseName());
                    inventoryRepository.save(inventory);

                    createInventoryItem(
                            inventory,
                            itemReq.getLocationItemId(),
                            itemReq.getProductColorId(),
                            itemReq.getQuantity());
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

        // if (request.getType() == EnumTypes.EXPORT || request.getType() ==
        // EnumTypes.TRANSFER) {
        // try {
        // Inventory finalInventory =
        // inventoryRepository.findByIdWithItems(inventory.getId())
        // .orElse(inventory);
        //
        // String pdfUrl = pdfService.generateExportPDF(finalInventory);
        // inventory.setPdfUrl(pdfUrl);
        // inventoryRepository.save(inventory);
        //
        // } catch (Exception e) {
        // log.error("Error generating PDF: {}", e.getMessage());
        // }
        // }

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
        if (transferStatus.equals(TransferStatus.ACCEPTED)) {
            transfer.setTransferStatus(TransferStatus.ACCEPTED);
            inventoryRepository.save(transfer);
            return mapToInventoryResponse(transfer);
        }
        if (transferStatus.equals(TransferStatus.FINISHED)) {
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
                "Import stock");

        createInventoryItem(inventory, request.getLocationItemId(), request.getProductColorId(), request.getQuantity());
        return mapToInventoryResponse(inventory);
    }

    @Override
    @Transactional
    public InventoryResponse exportStock(InventoryItemRequest request, String warehouseId) {

        List<InventoryItem> items = inventoryItemRepository
                .findAllByProductColorIdAndInventory_Warehouse_Id(request.getProductColorId(), warehouseId);

        if (items.isEmpty())
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);

        int remaining = request.getQuantity();

        for (InventoryItem item : items) {
            int available = item.getQuantity() - item.getReservedQuantity();
            if (available <= 0)
                continue;

            int toExport = Math.min(available, remaining);
            item.setQuantity(item.getQuantity() - toExport);
            inventoryItemRepository.save(item);

            remaining -= toExport;
            if (remaining <= 0)
                break;
        }

        if (remaining > 0)
            throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

        Inventory inventory = createInventory(
                warehouseId,
                EnumTypes.EXPORT,
                EnumPurpose.STOCK_OUT,
                "Export stock");

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
                "Transfer OUT to " + toWarehouse.getWarehouseName() + " / Zone: " + toZone.getZoneName());

        Inventory importInventory = createInventory(
                toWarehouse.getId(),
                EnumTypes.TRANSFER,
                EnumPurpose.MOVE,
                "Transfer IN from " + fromWarehouse.getWarehouseName() + " / Zone: " + fromZone.getZoneName());

        if (request.getItems() != null && !request.getItems().isEmpty()) {
            for (InventoryItemRequest item : request.getItems()) {
                if (!hasSufficientStock(item.getProductColorId(), fromWarehouse.getId(), item.getQuantity()))
                    throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

                createInventoryItem(exportInventory, fromLocation.getId(), item.getProductColorId(),
                        -item.getQuantity());
                createInventoryItem(importInventory, toLocation.getId(), item.getProductColorId(), item.getQuantity());
            }
        } else {
            if (!hasSufficientStock(request.getProductColorId(), fromWarehouse.getId(), request.getQuantity()))
                throw new AppException(ErrorCode.NOT_ENOUGH_QUANTITY);

            createInventoryItem(exportInventory, fromLocation.getId(), request.getProductColorId(),
                    -request.getQuantity());
            createInventoryItem(importInventory, toLocation.getId(), request.getProductColorId(),
                    request.getQuantity());
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

    @SuppressWarnings("unused")
    private void preloadWarehouseNames(
            Map<Warehouse, List<InventoryItem>> warehouseMap,
            Map<String, String> warehouseNameCache) {
        for (Warehouse w : warehouseMap.keySet()) {
            String id = w.getId();
            if (!warehouseNameCache.containsKey(id)) {
                try {
                    var resp = storeClient.getStoreById(w.getStoreId());
                    if (resp != null && resp.getData() != null) {
                        warehouseNameCache.put(id, resp.getData().getName());
                    } else {
                        warehouseNameCache.put(id, "Kho " + id);
                    }
                } catch (Exception e) {
                    warehouseNameCache.put(id, "Kho " + id);
                }
            }
        }
    }

    @Override
    @Transactional
    public ReserveStockResponse reserveStock(String productColorId, int quantity, long orderId) {

        OrderResponse order = getOrder(orderId);

        Warehouse assignedWarehouse = warehouseRepository.findByStoreIdAndIsDeletedFalse(order.getStoreId())
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));

        String assignedWarehouseId = assignedWarehouse.getId();

        List<Warehouse> warehouses = warehouseRepository.findAllOrderByPriority(assignedWarehouseId);

        List<WarehouseReserveInfo> localReservations = new ArrayList<>();
        int totalReserved = 0;

        for (Warehouse wh : warehouses) {
            boolean isAssigned = wh.getId().equals(assignedWarehouseId);
            int reservedHere = reserveAtWarehouse(
                    wh,
                    productColorId,
                    quantity - totalReserved,
                    orderId,
                    isAssigned);

            if (reservedHere > 0) {
                totalReserved += reservedHere;
                localReservations.add(
                        WarehouseReserveInfo.builder()
                                .warehouseId(wh.getId())
                                .warehouseName(wh.getWarehouseName())
                                .reservedQuantity(reservedHere)
                                .isAssignedWarehouse(isAssigned)
                                .build());
            }

            if (totalReserved >= quantity)
                break;
        }

        // 2. Consolidate ALL participating stores for this ORDER (not just this
        // product)
        List<Inventory> allTickets = inventoryRepository.findAllByOrderId(orderId).stream()
                .filter(t -> t.getType() == EnumTypes.RESERVE)
                .collect(Collectors.toList());

        List<WarehouseReserveInfo> aggregatedGlobalList = new ArrayList<>();

        for (Inventory ticket : allTickets) {
            String whId = ticket.getWarehouse().getId();
            int whTotalQty = ticket.getInventoryItems().stream().mapToInt(InventoryItem::getQuantity).sum();

            aggregatedGlobalList.add(WarehouseReserveInfo.builder()
                    .warehouseId(whId)
                    .warehouseName(ticket.getWarehouse().getWarehouseName())
                    .reservedQuantity(whTotalQty)
                    .isAssignedWarehouse(whId.equals(assignedWarehouseId))
                    .build());
        }

        StringBuilder globalOverview = new StringBuilder("\n--- TỔNG THỂ GIỮ HÀNG (TOÀN ĐƠN) ---\n");
        for (WarehouseReserveInfo info : aggregatedGlobalList) {
            globalOverview.append(String.format(
                    "%s: %d cái%s\n",
                    info.getWarehouseName(),
                    info.getReservedQuantity(),
                    info.isAssignedWarehouse() ? " (Kho được assign - ưu tiên)" : ""));
        }

        // 3. Update ALL tickets for this order so their DB record shows full
        // participation
        for (Inventory ticket : allTickets) {
            ticket.getReservedWarehouses().clear();
            for (WarehouseReserveInfo info : aggregatedGlobalList) {
                ticket.getReservedWarehouses().add(InventoryReservedWarehouse.builder()
                        .warehouseId(info.getWarehouseId())
                        .warehouseName(info.getWarehouseName())
                        .reservedQuantity(info.getReservedQuantity())
                        .orderId(orderId)
                        .inventory(ticket)
                        .isAssignedWarehouse(info.isAssignedWarehouse())
                        .build());
            }
        }
        inventoryRepository.saveAll(allTickets);

        Map<String, String> printContentByWarehouse = new HashMap<>();
        for (WarehouseReserveInfo info : aggregatedGlobalList) {
            // Find the specific reservation for THIS product in this warehouse (if any)
            int currentProductReservedHere = 0;
            for (WarehouseReserveInfo pInfo : localReservations) {
                if (pInfo.getWarehouseId().equals(info.getWarehouseId())) {
                    currentProductReservedHere = pInfo.getReservedQuantity();
                    break;
                }
            }

            String perWh = "PHIẾU GIỮ HÀNG CHO KHO: " + info.getWarehouseName() + "\n"
                    + "Đơn hàng: " + orderId + "\n"
                    + "Sản phẩm hiện tại: " + productColorId + "\n"
                    + "Số lượng [Sản phẩm này] kho này giữ: " + currentProductReservedHere + "\n"
                    + (info.isAssignedWarehouse() ? "→ Đây là kho được assign, ưu tiên.\n" : "")
                    + globalOverview;

            printContentByWarehouse.put(info.getWarehouseId(), perWh);
        }

        return ReserveStockResponse.builder()
                .orderId(orderId)
                .productColorId(productColorId)
                .totalNeeded(quantity)
                .totalReserved(totalReserved)
                .globalReservations(aggregatedGlobalList)
                .warehousePrintContentMap(printContentByWarehouse)
                .build();
    }

    private int reserveAtWarehouse(
            Warehouse warehouse,
            String productColorId,
            int needQty,
            long orderId,
            boolean isAssigned) {
        if (needQty <= 0)
            return 0;

        List<InventoryItem> items = inventoryItemRepository
                .findFullByProductColorIdAndWarehouseId(productColorId, warehouse.getId());

        int reservedHere = 0;
        Map<String, Integer> takenPerColor = new HashMap<>();

        for (InventoryItem item : items) {
            if (needQty <= 0)
                break;

            int available = item.getQuantity() - item.getReservedQuantity();
            if (available <= 0)
                continue;

            int take = Math.min(available, needQty);

            item.setReservedQuantity(item.getReservedQuantity() + take);

            needQty -= take;
            reservedHere += take;
            takenPerColor.merge(productColorId, take, Integer::sum);
        }

        if (reservedHere <= 0)
            return 0;

        inventoryItemRepository.saveAll(items);

        Inventory ticket = inventoryRepository.findByOrderIdAndWarehouseId(orderId, warehouse.getId())
                .orElseGet(() -> Inventory.builder()
                        .employeeId("SYSTEM_AUTO")
                        .type(EnumTypes.RESERVE)
                        .purpose(EnumPurpose.RESERVE)
                        .date(LocalDate.now())
                        .orderId(orderId)
                        .warehouse(warehouse)
                        .transferStatus(TransferStatus.FINISHED)
                        .note("Phiếu giữ hàng tự động cho đơn: " + orderId)
                        .inventoryItems(new ArrayList<>())
                        .reservedWarehouses(new ArrayList<>())
                        .build());

        // Update or add InventoryItems for this product
        boolean itemExists = false;
        for (InventoryItem ticketItem : ticket.getInventoryItems()) {
            if (ticketItem.getProductColorId().equals(productColorId)) {
                ticketItem.setQuantity(ticketItem.getQuantity() + reservedHere);
                itemExists = true;
                break;
            }
        }

        if (!itemExists) {
            ticket.getInventoryItems().add(
                    InventoryItem.builder()
                            .productColorId(productColorId)
                            .quantity(reservedHere)
                            .inventory(ticket)
                            .build());
        }

        inventoryRepository.save(ticket);

        return reservedHere;
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
                .build();
    }

    @Override
    @Transactional
    public void rollbackInventoryTicket(Long orderId) {
        log.info("🔍 Bắt đầu rollback inventory cho order: {}", orderId);

        List<Inventory> tickets = inventoryRepository.findAllByOrderId(orderId);

        if (tickets == null || tickets.isEmpty()) {
            log.warn(
                    "🛑 Không tìm thấy ticket nào cho order {}. Có thể order chưa được tạo ticket hoặc đã được rollback trước đó. Tiếp tục cancellation.",
                    orderId);
            // ✅ KHÔNG throw exception - Cho phép order cancellation tiếp tục
            // Vì có thể order chưa được assign/store accept nên chưa có ticket
            return;
        }

        log.info("🔍 Bắt đầu rollback {} ticket(s) cho order {}", tickets.size(), orderId);

        for (Inventory ticket : tickets) {

            if (ticket.getTransferStatus() != TransferStatus.FINISHED) {
                log.warn("⛔ Skip ticket {} vì status {}",
                        ticket.getId(), ticket.getTransferStatus());
                continue;
            }

            String warehouseId = ticket.getWarehouse().getId();
            log.info("📦 Rollback ticket {} tại kho {}", ticket.getId(), warehouseId);

            for (InventoryItem ticketItem : ticket.getInventoryItems()) {

                String productColorId = ticketItem.getProductColorId();
                int qtyToRelease = ticketItem.getQuantity();

                List<InventoryItem> stockItems = inventoryItemRepository.findFullByProductColorIdAndWarehouseId(
                        productColorId, warehouseId);

                int remaining = qtyToRelease;

                for (InventoryItem stockItem : stockItems) {
                    if (remaining <= 0)
                        break;

                    int reserved = stockItem.getReservedQuantity();
                    if (reserved <= 0)
                        continue;

                    int release = Math.min(reserved, remaining);

                    stockItem.setQuantity(stockItem.getQuantity() + release);
                    stockItem.setReservedQuantity(reserved - release);

                    remaining -= release;

                    log.info("♻️ Trả lại {} cho stockItem {} (quantity: {} -> {}, reserved: {} -> {})",
                            release, stockItem.getId(),
                            stockItem.getQuantity() - release, stockItem.getQuantity(),
                            reserved, stockItem.getReservedQuantity());
                }

                inventoryItemRepository.saveAll(stockItems);

                if (remaining > 0) {
                    log.error("❌ Rollback thiếu {} cho productColor {}", remaining, productColorId);
                } else {
                    log.info("✅ Hoàn trả {} cho productColor {} thành công", qtyToRelease, productColorId);
                }
            }

            // ✅ Xóa ticket – cascade xử lý phần còn lại
            inventoryRepository.delete(ticket);
            log.info("🗑 Đã rollback & xóa ticket {}", ticket.getId());
        }

        log.info("🎉 Rollback hoàn tất cho order {} - Đã xử lý {} kho", orderId, tickets.size());
    }

    // ----------------- CHECK STOCK -----------------

    @Override
    public boolean hasSufficientStock(String productColorId, String warehouseId, int requiredQty) {
        List<InventoryItem> items = inventoryItemRepository
                .findAllByProductColorIdAndInventory_Warehouse_Id(productColorId, warehouseId);
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
            EnumTypes.TRANSFER);

    private static final List<EnumTypes> VIRTUAL_STOCK_TYPES = List.of(
            EnumTypes.RESERVE,
            EnumTypes.EXPORT,
            EnumTypes.TRANSFER);

    @Override
    public int getTotalStockByProductColorId(String productColorId) {
        return Objects.requireNonNullElse(
                inventoryItemRepository.calculateTotalPhysicalStock(productColorId, VIRTUAL_STOCK_TYPES),
                0);
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
    public InventoryWarehouseViewResponse getWarehouseInventoryView(String warehouseId) {

        List<InventoryResponse> localResponses = inventoryRepository
                .findAllByWarehouse_Id(warehouseId)
                .stream()
                .map(this::mapToInventoryResponse)
                .toList();

        List<InventoryResponse> globalResponses = inventoryRepository
                .findAllByType(EnumTypes.RESERVE)
                .stream()
                .map(this::mapToInventoryResponse)
                .toList();

        return InventoryWarehouseViewResponse.builder()
                .warehouseId(warehouseId)
                .localTickets(localResponses)
                .globalTickets(globalResponses)
                .build();
    }

    // @Override
    // public List<InventoryResponse> getInventoryByWarehouse(String warehouseId) {
    // List<InventoryReservedWarehouse> reservedList =
    // inventoryReservedWarehouseRepository.findByWarehouseIdWithInventory(warehouseId);
    //
    // return reservedList.stream()
    // .map(rw -> {
    // InventoryResponse resp = mapToInventoryResponse(rw.getInventory());
    //
    // if (resp.getReservedWarehouses() == null) {
    // resp.setReservedWarehouses(new ArrayList<>());
    // }
    //
    // resp.getReservedWarehouses().add(
    // WarehouseReserveInfo.builder()
    // .warehouseId(rw.getWarehouseId())
    // .warehouseName(rw.getWarehouseName())
    // .reservedQuantity(rw.getReservedQuantity())
    // .build()
    // );
    //
    // return resp;
    // })
    // .collect(Collectors.toList());
    // }

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
                        EnumPurpose.REQUEST)
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
            if (available <= 0)
                continue;

            LocationItem li = it.getLocationItem();
            Zone zone = li.getZone();
            Warehouse w = zone.getWarehouse();

            grouped.computeIfAbsent(li.getId(), k -> ProductLocationResponse.LocationInfo.builder()
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

        List<InventoryItem> items = inventoryItemRepository.findFullByProductColorIdAndWarehouseId(productColorId,
                storeWarehouse.getId());

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
        } else if (inventory.getType() != EnumTypes.TRANSFER && inventory.getType() != EnumTypes.EXPORT) {
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

        // Map Items
        List<InventoryItemResponse> itemResponseList = Optional.ofNullable(inventory.getInventoryItems())
                .orElse(Collections.emptyList())
                .stream()
                .map(item -> InventoryItemResponse.builder()
                        .id(item.getId())
                        .quantity(item.getQuantity())
                        .reservedQuantity(item.getReservedQuantity())
                        .productColorId(item.getProductColorId())
                        .productName(getProductName(item.getProductColorId()).getProduct().getName())
                        .locationId(item.getLocationItem() != null ? item.getLocationItem().getId() : null)
                        .inventoryId(item.getInventory().getId())
                        .build())
                .collect(Collectors.toList());

        Map<String, WarehouseReserveInfo> warehouseMap = new HashMap<>();

        Optional.ofNullable(inventory.getReservedWarehouses())
                .orElse(Collections.emptyList())
                .forEach(rw -> warehouseMap.put(rw.getWarehouseId(),
                        WarehouseReserveInfo.builder()
                                .warehouseId(rw.getWarehouseId())
                                .warehouseName(rw.getWarehouseName())
                                .reservedQuantity(rw.getReservedQuantity())
                                // --- FIX: Đọc từ DB lên Response ---
                                .isAssignedWarehouse(rw.getIsAssignedWarehouse())
                                .build()));

        if (inventory.getWarehouse() != null) {
            warehouseMap.computeIfAbsent(inventory.getWarehouse().getId(),
                    id -> WarehouseReserveInfo.builder()
                            .warehouseId(id)
                            .warehouseName(inventory.getWarehouse().getWarehouseName())
                            .reservedQuantity(0)
                            .isAssignedWarehouse(false)
                            .build());
        }

        List<WarehouseReserveInfo> reservedWarehouses = new ArrayList<>(warehouseMap.values());

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
                .warehouseId(inventory.getWarehouse() != null ? inventory.getWarehouse().getId() : null)
                .warehouseName(inventory.getWarehouse() != null ? inventory.getWarehouse().getWarehouseName() : null)
                .toWarehouseId(inventory.getToWarehouseId())
                .toWarehouseName(inventory.getToWarehouseName())
                .itemResponseList(itemResponseList)
                .reservedWarehouses(reservedWarehouses)
                .build();
    }

    @SuppressWarnings("unused")
    private String extractProductIdFromCode(String code) {
        try {
            if (code != null && code.startsWith("RES-")) {
                String[] parts = code.split("-");
                if (parts.length >= 3) {
                    return parts[2];
                }
            }
        } catch (Exception e) {
        }
        return null;
    }

    @SuppressWarnings("unused")
    private int parseQuantityFromNote(String note) {
        try {
            if (note != null && note.contains("Reserved:")) {
                String[] parts = note.split(" ");
                for (String part : parts) {
                    if (part.matches("\\d+"))
                        return Integer.parseInt(part);
                }
            }
        } catch (Exception ignored) {
        }
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

    private ProductColorResponse getProductName(String productColorId) {
        ProductColorResponse response = productServiceClient.getProductColor(productColorId);
        if (response == null) {
            throw new AppException(ErrorCode.PRODUCT_NOT_FOUND);
        }
        return response;
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

    private OrderResponse getOrder(long orderId) {
        if (orderId <= 0) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        ApiResponse<OrderResponse> response = orderClient.getOderById(orderId);

        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.ORDER_NOT_FOUND);
        }

        return response.getData();
    }

    private String getProfile() {
        ApiResponse<UserResponse> response = userClient.getEmployeeProfile();
        if (response == null || response.getData() == null) {
            throw new AppException(ErrorCode.NOT_FOUND_USER);
        }
        return response.getData().getId();
    }

    @Override
    @Transactional
    public void cancelInventoryTicket(Long ticketId) {
        log.info("🔍 Bắt đầu hủy phiếu giữ hàng lẻ: {}", ticketId);

        Inventory ticket = inventoryRepository.findById(ticketId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (ticket.getTransferStatus() != TransferStatus.FINISHED) {
            log.warn("⛔ Không thể hủy ticket {} vì trạng thái không phải FINISHED (status: {})",
                    ticket.getId(), ticket.getTransferStatus());
            return;
        }

        String warehouseId = ticket.getWarehouse().getId();
        log.info("📦 Rollback ticket {} tại kho {}", ticket.getId(), warehouseId);

        for (InventoryItem ticketItem : ticket.getInventoryItems()) {
            String productColorId = ticketItem.getProductColorId();
            int qtyToRelease = ticketItem.getQuantity();

            List<InventoryItem> stockItems = inventoryItemRepository.findFullByProductColorIdAndWarehouseId(
                    productColorId, warehouseId);

            int remaining = qtyToRelease;

            for (InventoryItem stockItem : stockItems) {
                if (remaining <= 0)
                    break;

                int reserved = stockItem.getReservedQuantity();
                if (reserved <= 0)
                    continue;

                int release = Math.min(reserved, remaining);

                stockItem.setQuantity(stockItem.getQuantity() + release);
                stockItem.setReservedQuantity(reserved - release);

                remaining -= release;

                log.info("♻️ Trả lại {} cho stockItem {} (quantity: {}, reserved: {})",
                        release, stockItem.getId(), stockItem.getQuantity(), stockItem.getReservedQuantity());
            }

            inventoryItemRepository.saveAll(stockItems);

            if (remaining > 0) {
                log.error("❌ Hủy phiếu thiếu {} cho productColor {}", remaining, productColorId);
            }
        }

        inventoryRepository.delete(ticket);
        log.info("🗑 Đã xóa ticket {}", ticket.getId());
    }

}
