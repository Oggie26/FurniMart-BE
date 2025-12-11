package com.example.inventoryservice.controller;

import com.example.inventoryservice.enums.TransferStatus;
import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.*;
import com.example.inventoryservice.service.PDFService;
import com.example.inventoryservice.service.inteface.InventoryService;
import com.example.inventoryservice.repository.InventoryRepository;
import com.example.inventoryservice.enums.ErrorCode;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller", description = "API quản lý phiếu kho, tồn kho, giao dịch")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;
    private final PDFService pdfService;
    private final InventoryRepository inventoryRepository;

    // ==========================================================
    // 1. QUẢN LÝ PHIẾU KHO
    // ==========================================================

    @Operation(summary = "Tạo hoặc Cập nhật phiếu kho")
    @PostMapping
    public ApiResponse<InventoryResponse> createOrUpdateInventory(
            @Valid @RequestBody InventoryRequest request) {

        InventoryResponse response = inventoryService.createOrUpdateInventory(request);

        return ApiResponse.<InventoryResponse>builder()
                .status(200)
                .message("Tạo/Cập nhật phiếu kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Thêm Chi Tiết Item vào Phiếu Kho")
    @PostMapping("/inventory/{inventoryId}/items")
    public ApiResponse<InventoryItemResponse> addInventoryItem(
            @PathVariable Long inventoryId,
            @Valid @RequestBody InventoryItemRequest request) {

        InventoryItemResponse response = inventoryService.addInventoryItem(request, inventoryId);

        return ApiResponse.<InventoryItemResponse>builder()
                .status(200)
                .message("Thêm chi tiết vào phiếu kho thành công")
                .data(response)
                .build();
    }

    // ==========================================================
    // 2. NGHIỆP VỤ KHO
    // ==========================================================

    @Operation(summary = "Nhập kho (Tạo phiếu IMPORT)")
    @PostMapping("/{warehouseId}/import")
    public ApiResponse<InventoryResponse> importStock(
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryItemRequest request) {

        InventoryResponse response = inventoryService.importStock(request, warehouseId);

        return ApiResponse.<InventoryResponse>builder()
                .status(200)
                .message("Nhập kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Xuất kho (Tạo phiếu EXPORT)")
    @PostMapping("/{warehouseId}/export")
    public ApiResponse<InventoryResponse> exportStock(
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryItemRequest request) {

        InventoryResponse response = inventoryService.exportStock(request, warehouseId);

        return ApiResponse.<InventoryResponse>builder()
                .status(200)
                .message("Xuất kho thành công")
                .data(response)
                .build();
    }

    @PostMapping("/reserve/{orderId}")
    public ApiResponse<ReserveStockResponse> reserveStock(
            @PathVariable Long orderId,
            @RequestParam("productColorId") @NotBlank String productColorId,
            @RequestParam("quantity") @Min(1) int quantity)  {

        ReserveStockResponse response = inventoryService.reserveStock(
                productColorId,
                quantity,
                orderId
        );

        return ApiResponse.<ReserveStockResponse>builder()
                .status(200)
                .message("Giữ hàng thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Bỏ giữ hàng (Tạo phiếu RELEASE)")
    @PostMapping("/release/{orderId}")
    public ApiResponse<ReserveStockResponse> releaseReservedStock(
            @Valid @RequestBody InventoryItemRequest request, @PathVariable Long orderId) {

        ReserveStockResponse response = inventoryService.releaseReservedStock(
                request.getProductColorId(),
                request.getQuantity(),
                orderId
        );

        return ApiResponse.<ReserveStockResponse>builder()
                .status(200)
                .message("Bỏ giữ hàng thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Chuyển kho (Tạo phiếu TRANSFER)")
    @PostMapping("/transfer")
    public ApiResponse<Void> transferStock(
            @Valid @RequestBody TransferStockRequest request) {

        inventoryService.transferStock(request);

        return ApiResponse.<Void>builder()
                .status(200)
                .message("Chuyển kho thành công")
                .build();
    }

    // ==========================================================
    // 3. TRUY VẤN TỒN KHO
    // ==========================================================

    @Operation(summary = "Kiểm tra tồn kho theo kho")
    @GetMapping("/stock/check-warehouse")
    public ApiResponse<Boolean> hasSufficientStock(
            @RequestParam @NotBlank String productColorId,
            @RequestParam @NotBlank String warehouseId,
            @RequestParam @Min(1) int requiredQty) {

        boolean response = inventoryService.hasSufficientStock(productColorId, warehouseId, requiredQty);

        return ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Kiểm tra tồn kho toàn hệ thống")
    @GetMapping("/stock/check-global")
    public ApiResponse<Boolean> hasSufficientGlobalStock(
            @RequestParam @NotBlank String productColorId,
            @RequestParam @Min(1) int requiredQty) {

        boolean response = inventoryService.hasSufficientGlobalStock(productColorId, requiredQty);

        return ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho toàn cục thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy tổng tồn kho vật lý")
    @GetMapping("/stock/total-physical")
    public ApiResponse<Integer> getTotalStockByProductColorId(
            @RequestParam @NotBlank String productColorId) {

        return ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho vật lý thành công")
                .data(inventoryService.getTotalStockByProductColorId(productColorId))
                .build();
    }

    @Operation(summary = "Lấy tổng tồn kho khả dụng")
    @GetMapping("/stock/total-available")
    public ApiResponse<Integer> getAvailableStockByProductColorId(
            @RequestParam @NotBlank String productColorId) {

        return ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho khả dụng thành công")
                .data(inventoryService.getAvailableStockByProductColorId(productColorId))
                .build();
    }

    @Operation(summary = "Kiểm tra sức chứa Zone")
    @GetMapping("/zone/{zoneId}/check-capacity")
    public ApiResponse<Boolean> checkZoneCapacity(
            @PathVariable @NotBlank String zoneId,
            @RequestParam @Min(1) int additionalQty) {

        boolean response = inventoryService.checkZoneCapacity(zoneId, additionalQty);
        return ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra sức chứa Zone thành công")
                .data(response)
                .build();
    }

    // ==========================================================
    // 4. TRUY VẤN DANH SÁCH & LỊCH SỬ
    // ==========================================================

    @Operation(summary = "Duyệt hoặc Từ chối phiếu chuyển kho")
    @PostMapping("/transfer/{inventoryId}/approve")
    public ApiResponse<InventoryResponse> approveTransfer(
            @PathVariable String inventoryId,
            @RequestParam TransferStatus transferStatus) {

        InventoryResponse response = inventoryService.approveTransfer(inventoryId, transferStatus);

        return ApiResponse.<InventoryResponse>builder()
                .status(200)
                .message("Duyệt chuyển kho thành công" )
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách sản phẩm còn hàng theo Store ID")
    @GetMapping("/stock/by-store")
    public ApiResponse<ProductLocationResponse> getProductByStoreId(
            @RequestParam @NotBlank String storeId) {

        ProductLocationResponse response = inventoryService.getProductByStoreId(storeId);

        return ApiResponse.<ProductLocationResponse>builder()
                .status(200)
                .message("Lấy sản phẩm theo store thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy tất cả Phiếu Kho")
    @GetMapping
    public ApiResponse<List<InventoryResponse>> getAllInventories() {
        List<InventoryResponse> inventories = inventoryService.getAllInventories();

        return ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy tất cả phiếu kho thành công")
                .data(inventories)
                .build();
    }

    @Operation(summary = "Lấy tất cả kho chứa productColorId")
    @GetMapping("/stock/locations/all")
    public ApiResponse<ProductLocationResponse> getAllProductLocations(
            @RequestParam @NotBlank String productColorId) {

        ProductLocationResponse response = inventoryService.getAllProductLocations(productColorId);

        return ApiResponse.<ProductLocationResponse>builder()
                .status(200)
                .message("Lấy toàn bộ vị trí sản phẩm thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy vị trí chứa productColorId theo Warehouse ID")
    @GetMapping("/stock/locations/by-warehouse")
    public ApiResponse<ProductLocationResponse> getProductLocationsByWarehouse(
            @RequestParam @NotBlank String productColorId,
            @RequestParam @NotBlank String storeId) {

        ProductLocationResponse response =
                inventoryService.getProductLocationsByWarehouse(productColorId, storeId);

        return ApiResponse.<ProductLocationResponse>builder()
                .status(200)
                .message("Lấy vị trí sản phẩm theo warehouse thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy Phiếu Kho theo ID")
    @GetMapping("/{inventoryId}")
    public ApiResponse<InventoryResponse> getInventoryById(@PathVariable Long inventoryId) {

        InventoryResponse response = inventoryService.getInventoryById(inventoryId);

        return ApiResponse.<InventoryResponse>builder()
                .status(200)
                .message("Lấy phiếu kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách Phiếu Kho theo Warehouse ID")
    @GetMapping("/warehouse/{warehouseId}")
    public ApiResponse<List<InventoryResponse>> getInventoryByWarehouse(@PathVariable String warehouseId) {

        List<InventoryResponse> response = inventoryService.getInventoryByWarehouse(warehouseId);

        return ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách phiếu kho theo kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách Phiếu Kho theo Zone ID")
    @GetMapping("/zone/{zoneId}")
    public ApiResponse<List<InventoryResponse>> getInventoryByZone(@PathVariable String zoneId) {

        List<InventoryResponse> response = inventoryService.getInventoryByZone(zoneId);

        return ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách phiếu kho theo Zone thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy tất cả Chi Tiết Phiếu Kho")
    @GetMapping("/items")
    public ApiResponse<List<InventoryItemResponse>> getAllInventoryItems() {

        List<InventoryItemResponse> items = inventoryService.getAllInventoryItems();

        return ApiResponse.<List<InventoryItemResponse>>builder()
                .status(200)
                .message("Lấy tất cả lịch sử giao dịch thành công")
                .data(items)
                .build();
    }

    @Operation(summary = "Lấy Chi Tiết Giao Dịch theo Product ID")
    @GetMapping("/items/product/{productColorId}")
    public ApiResponse<List<InventoryItemResponse>> getInventoryItemsByProduct(
            @PathVariable String productColorId) {

        List<InventoryItemResponse> response = inventoryService.getInventoryItemsByProduct(productColorId);

        return ApiResponse.<List<InventoryItemResponse>>builder()
                .status(200)
                .message("Lấy danh sách giao dịch theo sản phẩm thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy lịch sử giao dịch theo sản phẩm (tùy chọn Zone)")
    @GetMapping("/items/history")
    public ApiResponse<List<InventoryItemResponse>> getTransactionHistory(
            @RequestParam @NotBlank String productColorId,
            @RequestParam(required = false) String zoneId) {

        List<InventoryItemResponse> response = inventoryService.getTransactionHistory(productColorId, zoneId);

        return ApiResponse.<List<InventoryItemResponse>>builder()
                .status(200)
                .message("Lấy lịch sử giao dịch thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy danh sách các yêu cầu chuyển kho đang chờ duyệt cho kho")
    @GetMapping("/transfer/pending/{warehouseId}")
    public ApiResponse<List<InventoryResponse>> getPendingTransferRequests(
            @PathVariable String warehouseId) {

        List<InventoryResponse> response = inventoryService.getPendingTransfers(warehouseId);

        return ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách yêu cầu chuyển kho đang chờ duyệt thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Tạo PDF phiếu xuất kho")
    @PostMapping("/{inventoryId}/generate-pdf")
    @Transactional(readOnly = true)
    public ApiResponse<String> generateExportPDF(@PathVariable Long inventoryId) {
        var inventory = inventoryRepository.findById(inventoryId)
                .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND));

        if (inventory.getType() != com.example.inventoryservice.enums.EnumTypes.EXPORT) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }

        if (inventory.getWarehouse() != null) {
            inventory.getWarehouse().getWarehouseName();
        }
        if (inventory.getInventoryItems() != null) {
            inventory.getInventoryItems().forEach(item -> {
                if (item.getLocationItem() != null) {
                    item.getLocationItem().getCode(); // Trigger lazy load
                }
            });
        }

        String pdfPath = pdfService.generateExportPDF(inventory);

        return ApiResponse.<String>builder()
                .status(200)
                .message("Tạo PDF phiếu xuất kho thành công")
                .data(pdfPath)
                .build();
    }

    @Operation(summary = "Lấy danh sách sản phẩm sắp hết hàng (Low Stock Alert)")
    @GetMapping("/stock/low-stock")
    public ApiResponse<List<LowStockAlertResponse>> getLowStockProducts(
            @RequestParam(name = "storeId", required = false) String storeId,
            @RequestParam(name = "threshold", required = false, defaultValue = "10") @Min(1) Integer threshold) {
        
        // TODO: Filter by storeId when service method supports it
        // Currently service method doesn't support storeId filtering
        List<LowStockAlertResponse> alerts = inventoryService.getLowStockProducts(threshold);
        
        return ApiResponse.<List<LowStockAlertResponse>>builder()
                .status(200)
                .message("Lấy danh sách sản phẩm sắp hết hàng thành công")
                .data(alerts)
                .build();
    }

    @Operation(summary = "Lấy danh sách phiếu giữ hàng (Pending Reservation) theo Store ID")
    @GetMapping("/reserve/pending")
    public ApiResponse<List<InventoryResponse>> getPendingReservations(
            @RequestParam @NotBlank String storeId) {

        List<InventoryResponse> response = inventoryService.getPendingReservations(storeId);

        return ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách phiếu giữ hàng đang chờ xử lý thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Lấy view phiếu kho cho 1 warehouse (local + global RESERVE)")
    @GetMapping("/warehouse/view")
    public ApiResponse<InventoryWarehouseViewResponse> getWarehouseInventoryView(
            @RequestParam String warehouseId
    ) {
        InventoryWarehouseViewResponse response =
                inventoryService.getWarehouseInventoryView(warehouseId);

        return ApiResponse.<InventoryWarehouseViewResponse>builder()
                .status(200)
                .message("Lấy view phiếu kho thành công")
                .data(response)
                .build();
    }

    @Operation(summary = "Rollback phiếu kho cho một order")
    @DeleteMapping("/rollback/{orderId}")
    public ApiResponse<Void> rollbackInventory(@PathVariable Long orderId) {
        try {
            inventoryService.rollbackInventoryTicket(orderId);
            return ApiResponse.<Void>builder()
                    .status(200)
                    .message("Rollback inventory cho order " + orderId + " thành công")
                    .data(null)
                    .build();
        } catch (AppException e) {
            // Nếu có lỗi nghiệp vụ
            return ApiResponse.<Void>builder()
                    .status(400)
                    .message(e.getMessage())
                    .data(null)
                    .build();
        } catch (Exception e) {
            // Nếu lỗi hệ thống
            return ApiResponse.<Void>builder()
                    .status(500)
                    .message("Rollback thất bại: " + e.getMessage())
                    .data(null)
                    .build();
        }
    }

}
