package com.example.inventoryservice.controller;

import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.request.InventoryItemRequest;
import com.example.inventoryservice.request.InventoryRequest;
import com.example.inventoryservice.request.TransferStockRequest;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.InventoryItemResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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


    // =================================================================
    // 1. QUẢN LÝ PHIẾU KHO
    // =================================================================

    @Operation(summary = "Tạo hoặc Cập nhật một Phiếu Kho", description = "Tạo mới hoặc cập nhật phiếu kho dựa trên thông tin request")
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> createOrUpdateInventory(
            @Valid @RequestBody(description = "Thông tin phiếu kho") InventoryRequest request) {
        try {
            InventoryResponse response = inventoryService.createOrUpdateInventory(request);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Tạo/Cập nhật phiếu kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    @Operation(summary = "Thêm Chi Tiết Item vào Phiếu Kho", description = "Thêm một chi tiết item vào phiếu kho đã tồn tại")
    @PostMapping("/inventory/{inventoryId}/items")
    public ResponseEntity<ApiResponse<InventoryItemResponse>> addInventoryItem(
            @Valid @RequestBody(description = "Thông tin chi tiết item") InventoryItemRequest request, @PathVariable Long inventoryId) {
        try {
            InventoryItemResponse response = inventoryService.addInventoryItem(request, inventoryId);
            return ResponseEntity.ok(ApiResponse.<InventoryItemResponse>builder()
                    .status(200)
                    .message("Thêm chi tiết vào phiếu kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryItemResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    // =================================================================
    // 2. NGHIỆP VỤ KHO
    // =================================================================

    @Operation(summary = "Nhập kho (Tạo phiếu IMPORT)", description = "Tạo phiếu nhập kho và chi tiết item từ request")
    @PostMapping("/import")
    public ResponseEntity<ApiResponse<InventoryResponse>> importStock(
            @Valid @RequestBody(description = "Thông tin item nhập kho") InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.importStock(request);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Nhập kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    @Operation(summary = "Xuất kho (Tạo phiếu EXPORT)", description = "Tạo phiếu xuất kho và chi tiết item từ request")
    @PostMapping("/export")
    public ResponseEntity<ApiResponse<InventoryResponse>> exportStock(
            @Valid @RequestBody(description = "Thông tin item xuất kho") InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.exportStock(request);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Xuất kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    @Operation(summary = "Giữ hàng (Tạo phiếu RESERVE)", description = "Giữ số lượng hàng cho sản phẩm trong kho")
    @PostMapping("/reserve")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(
            @Valid @RequestBody(description = "Thông tin item cần giữ") InventoryItemRequest request) {
        try {
            InventoryResponse response = inventoryService.reserveStock(request.getProductColorId(), request.getQuantity());
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Giữ hàng thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    @Operation(
            summary = "Bỏ giữ hàng (Tạo phiếu RELEASE)",
            description = "Bỏ giữ số lượng hàng đã giữ trước đó"
    )
    @PostMapping("/release")
    public ResponseEntity<ApiResponse<InventoryResponse>> releaseReservedStock(
            @Valid @RequestBody InventoryItemRequest request) {
        try {
            // Gọi service với productColorId và quantity giống reserveStock
            InventoryResponse response = inventoryService.releaseReservedStock(
                    request.getProductColorId(), request.getQuantity()
            );
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Bỏ giữ hàng thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }


    @Operation(summary = "Chuyển kho (Tạo phiếu TRANSFER)", description = "Chuyển stock từ kho đi sang kho đến, tự tạo 2 phiếu TRANSFER")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Void>> transferStock(
            @Valid @RequestBody(description = "Thông tin chuyển kho") TransferStockRequest request) {
        try {
            inventoryService.transferStock(request);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .status(200)
                    .message("Chuyển kho thành công")
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    // =================================================================
    // 3. TRUY VẤN TỒN KHO
    // =================================================================

    @Operation(summary = "Kiểm tra tồn kho kho cụ thể", description = "Kiểm tra số lượng khả dụng tại 1 kho")
    @GetMapping("/stock/check-warehouse")
    public ResponseEntity<ApiResponse<Boolean>> hasSufficientStock(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @RequestParam @NotBlank String productColorId,
            @Parameter(description = "ID kho", required = true) @RequestParam @NotBlank String warehouseId,
            @Parameter(description = "Số lượng cần kiểm tra", required = true) @RequestParam @Min(1) int requiredQty) {
        boolean response = inventoryService.hasSufficientStock(productColorId, warehouseId, requiredQty);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho kho thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Kiểm tra tồn kho toàn hệ thống", description = "Kiểm tra tổng số lượng khả dụng cho sản phẩm trên toàn hệ thống")
    @GetMapping("/stock/check-global")
    public ResponseEntity<ApiResponse<Boolean>> hasSufficientGlobalStock(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @RequestParam @NotBlank String productColorId,
            @Parameter(description = "Số lượng cần kiểm tra", required = true) @RequestParam @Min(1) int requiredQty) {
        boolean response = inventoryService.hasSufficientGlobalStock(productColorId, requiredQty);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho toàn cục thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy tổng tồn kho vật lý", description = "Tổng quantity thực tế toàn hệ thống")
    @GetMapping("/stock/total-physical")
    public ResponseEntity<ApiResponse<Integer>> getTotalStockByProductColorId(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @RequestParam @NotBlank String productColorId) {
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho vật lý thành công")
                .data(inventoryService.getTotalStockByProductColorId(productColorId))
                .build());
    }

    @Operation(summary = "Lấy tổng tồn kho khả dụng", description = "Tổng quantity khả dụng = quantity - reserved toàn hệ thống")
    @GetMapping("/stock/total-available")
    public ResponseEntity<ApiResponse<Integer>> getAvailableStockByProductColorId(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @RequestParam @NotBlank String productColorId) {
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho khả dụng thành công")
                .data(inventoryService.getAvailableStockByProductColorId(productColorId))
                .build());
    }

    @Operation(summary = "Kiểm tra sức chứa Zone", description = "Kiểm tra Zone còn đủ chỗ cho số lượng bổ sung hay không")
    @GetMapping("/zone/{zoneId}/check-capacity")
    public ResponseEntity<ApiResponse<Boolean>> checkZoneCapacity(
            @Parameter(description = "ID Zone", required = true) @PathVariable @NotBlank String zoneId,
            @Parameter(description = "Số lượng bổ sung cần kiểm tra", required = true) @RequestParam @Min(1) int additionalQty) {
        try {
            boolean response = inventoryService.checkZoneCapacity(zoneId, additionalQty);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .status(200)
                    .message("Kiểm tra sức chứa Zone thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Boolean>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    // =================================================================
    // 4. TRUY VẤN DANH SÁCH & LỊCH SỬ
    // =================================================================

    @Operation(summary = "Lấy tất cả Phiếu Kho")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventories() {
        List<InventoryResponse> inventories = inventoryService.getAllInventories();
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy tất cả phiếu kho thành công")
                .data(inventories)
                .build());
    }

    @Operation(summary = "Lấy Phiếu Kho theo ID")
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(
            @Parameter(description = "ID phiếu kho", required = true) @PathVariable Long inventoryId) {
        try {
            InventoryResponse response = inventoryService.getInventoryById(inventoryId);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Lấy phiếu kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }

    @Operation(summary = "Lấy danh sách Phiếu Kho theo Warehouse ID")
    @GetMapping("/warehouse/{warehouseId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByWarehouse(
            @Parameter(description = "ID kho", required = true) @PathVariable @NotBlank String warehouseId) {
        List<InventoryResponse> response = inventoryService.getInventoryByWarehouse(warehouseId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách phiếu kho theo kho thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy danh sách Phiếu Kho theo Zone ID")
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByZone(
            @Parameter(description = "ID Zone", required = true) @PathVariable @NotBlank String zoneId) {
        List<InventoryResponse> response = inventoryService.getInventoryByZone(zoneId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách phiếu kho theo Zone thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy tất cả Chi Tiết Phiếu Kho")
    @GetMapping("/items")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getAllInventoryItems() {
        List<InventoryItemResponse> transactions = inventoryService.getAllInventoryItems();
        return ResponseEntity.ok(ApiResponse.<List<InventoryItemResponse>>builder()
                .status(200)
                .message("Lấy tất cả lịch sử giao dịch thành công")
                .data(transactions)
                .build());
    }

    @Operation(summary = "Lấy Chi Tiết Giao Dịch theo Product ID")
    @GetMapping("/items/product/{productColorId}")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getInventoryItemsByProduct(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @PathVariable @NotBlank String productColorId) {
        List<InventoryItemResponse> response = inventoryService.getInventoryItemsByProduct(productColorId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryItemResponse>>builder()
                .status(200)
                .message("Lấy danh sách giao dịch theo sản phẩm thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy lịch sử giao dịch theo sản phẩm (tùy chọn lọc theo Zone)")
    @GetMapping("/items/history")
    public ResponseEntity<ApiResponse<List<InventoryItemResponse>>> getTransactionHistory(
            @Parameter(description = "ID sản phẩm (ProductColorId)", required = true) @RequestParam @NotBlank String productColorId,
            @Parameter(description = "ID Zone (tùy chọn)") @RequestParam(required = false) String zoneId) {
        try {
            List<InventoryItemResponse> response = inventoryService.getTransactionHistory(productColorId, zoneId);
            return ResponseEntity.ok(ApiResponse.<List<InventoryItemResponse>>builder()
                    .status(200)
                    .message("Lấy lịch sử giao dịch thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<List<InventoryItemResponse>>builder()
                            .status(e.getErrorCode().getCode())
                            .message(e.getErrorCode().getMessage())
                            .build());
        }
    }
}
