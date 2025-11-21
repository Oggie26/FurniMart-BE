package com.example.inventoryservice.controller;

import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.InventoryItemResponse;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.ProductLocationResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller", description = "API quản lý phiếu kho, tồn kho, giao dịch")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    // ==========================================================
    // 1. QUẢN LÝ PHIẾU KHO
    // ==========================================================

    @Operation(summary = "Tạo hoặc Cập nhật phiếu kho")
    @PostMapping
    public ApiResponse<InventoryResponse> createOrUpdateInventory(
            @Valid @RequestBody InventoryRequest request) {
        try {
            InventoryResponse response = inventoryService.createOrUpdateInventory(request);
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Tạo/Cập nhật phiếu kho thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    @Operation(summary = "Thêm Chi Tiết Item vào Phiếu Kho")
    @PostMapping("/inventory/{inventoryId}/items")
    public ApiResponse<InventoryItemResponse> addInventoryItem(
            @PathVariable Long inventoryId,
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            InventoryItemResponse response = inventoryService.addInventoryItem(request, inventoryId);
            return ApiResponse.<InventoryItemResponse>builder()
                    .status(200)
                    .message("Thêm chi tiết vào phiếu kho thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryItemResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    // ==========================================================
    // 2. NGHIỆP VỤ KHO
    // ==========================================================

    @Operation(summary = "Nhập kho (Tạo phiếu IMPORT)")
    @PostMapping("/{warehouseId}/import")
    public ApiResponse<InventoryResponse> importStock(
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.importStock(request, warehouseId);
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Nhập kho thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    @Operation(summary = "Xuất kho (Tạo phiếu EXPORT)")
    @PostMapping("/{warehouseId}/export")
    public ApiResponse<InventoryResponse> exportStock(
            @PathVariable String warehouseId,
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.exportStock(request, warehouseId);
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Xuất kho thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    @Operation(summary = "Giữ hàng (Tạo phiếu RESERVE)")
    @PostMapping("/reserve")
    public ApiResponse<InventoryResponse> reserveStock(
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.reserveStock(
                    request.getProductColorId(),
                    request.getQuantity()
            );
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Giữ hàng thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    @Operation(summary = "Bỏ giữ hàng (Tạo phiếu RELEASE)")
    @PostMapping("/release")
    public ApiResponse<InventoryResponse> releaseReservedStock(
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.releaseReservedStock(
                    request.getProductColorId(),
                    request.getQuantity()
            );
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Bỏ giữ hàng thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    @Operation(summary = "Chuyển kho (Tạo phiếu TRANSFER)")
    @PostMapping("/transfer")
    public ApiResponse<Void> transferStock(
            @Valid @RequestBody TransferStockRequest request) {
        try {
            inventoryService.transferStock(request);
            return ApiResponse.<Void>builder()
                    .status(200)
                    .message("Chuyển kho thành công")
                    .build();
        } catch (AppException e) {
            return ApiResponse.<Void>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
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
        try {
            boolean response = inventoryService.checkZoneCapacity(zoneId, additionalQty);
            return ApiResponse.<Boolean>builder()
                    .status(200)
                    .message("Kiểm tra sức chứa Zone thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<Boolean>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }

    // ==========================================================
    // 4. TRUY VẤN DANH SÁCH & LỊCH SỬ
    // ==========================================================

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

    @Operation(summary = "Lấy tất cả vị trí chứa productColorId (warehouse, zone, location)")
    @GetMapping("/stock/locations")
    public ApiResponse<List<ProductLocationResponse>> getProductLocations(
            @RequestParam @NotBlank String productColorId) {
        try {
            List<ProductLocationResponse> response = inventoryService.getProductLocations(productColorId);
            return ApiResponse.<List<ProductLocationResponse>>builder()
                    .status(200)
                    .message("Lấy vị trí sản phẩm thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<List<ProductLocationResponse>>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }


    @Operation(summary = "Lấy Phiếu Kho theo ID")
    @GetMapping("/{inventoryId}")
    public ApiResponse<InventoryResponse> getInventoryById(@PathVariable Long inventoryId) {
        try {
            InventoryResponse response = inventoryService.getInventoryById(inventoryId);
            return ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Lấy phiếu kho thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<InventoryResponse>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
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
    public ApiResponse<List<InventoryItemResponse>> getInventoryItemsByProduct(@PathVariable String productColorId) {
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
        try {
            List<InventoryItemResponse> response = inventoryService.getTransactionHistory(productColorId, zoneId);
            return ApiResponse.<List<InventoryItemResponse>>builder()
                    .status(200)
                    .message("Lấy lịch sử giao dịch thành công")
                    .data(response)
                    .build();
        } catch (AppException e) {
            return ApiResponse.<List<InventoryItemResponse>>builder()
                    .status(e.getErrorCode().getCode())
                    .message(e.getErrorCode().getMessage())
                    .build();
        }
    }
}
