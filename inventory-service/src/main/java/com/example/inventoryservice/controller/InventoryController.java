package com.example.inventoryservice.controller;

import com.example.inventoryservice.exception.AppException;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.InventoryResponse;
import com.example.inventoryservice.response.InventoryTransactionResponse;
import com.example.inventoryservice.service.inteface.InventoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventories")
@RequiredArgsConstructor
@Tag(name = "Inventory Controller")
@Validated
public class InventoryController {

    private final InventoryService inventoryService;

    // --------------------------------------------------------------------------
    // 1. QUẢN LÝ TỒN KHO CƠ BẢN (PHYSICAL STOCK)
    // --------------------------------------------------------------------------

    @Operation(summary = "Tạo hoặc cập nhật inventory")
    @PostMapping
    public ResponseEntity<ApiResponse<InventoryResponse>> upsertInventory(
            @RequestParam @NotBlank(message = "Product ID is required") String productColorId,
            @RequestParam @NotBlank(message = "Location Item ID is required") String locationItemId,
            @RequestParam @NotNull(message = "Quantity is required") @Min(value = 0, message = "Quantity must be non-negative") int quantity,
            @RequestParam @NotNull(message = "Min Quantity is required") @Min(value = 0, message = "Min Quantity must be non-negative") int minQuantity,
            @RequestParam @NotNull(message = "Max Quantity is required") @Min(value = 0, message = "Max Quantity must be non-negative") int maxQuantity) {
        try {
            InventoryResponse response = inventoryService.upsertInventory(productColorId, locationItemId, quantity, minQuantity, maxQuantity);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Cập nhật hoặc tạo inventory thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Tăng tồn kho")
    @PatchMapping("/{warehouseId}/{productColorId}/{locationItemId}/increase")
    public ResponseEntity<ApiResponse<InventoryResponse>> increaseStock(
            @PathVariable @NotBlank(message = "Warehouse is required") String warehouseId,
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @PathVariable @NotBlank(message = "Location Item ID is required") String locationItemId,
            @RequestParam @NotNull(message = "Amount is required") @Min(value = 1, message = "Amount must be positive") int amount) {
        try {
            InventoryResponse response = inventoryService.increaseStock(productColorId, locationItemId, amount, warehouseId);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Tăng tồn kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Giảm tồn kho")
    @PatchMapping("/{warehouseId}/{productColorId}/{locationItemId}/decrease")
    public ResponseEntity<ApiResponse<InventoryResponse>> decreaseStock(
            @PathVariable @NotBlank(message = "Warehouse is required") String warehouseId,
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @PathVariable @NotBlank(message = "Location Item ID is required") String locationItemId,
            @RequestParam @NotNull(message = "Amount is required") @Min(value = 1, message = "Amount must be positive") int amount) {
        try {
            InventoryResponse response = inventoryService.decreaseStock(productColorId, locationItemId, amount,  warehouseId);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Giảm tồn kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    // --------------------------------------------------------------------------
    // 2. QUẢN LÝ DỰ TRỮ (STOCK RESERVATION) - BỔ SUNG MỚI
    // --------------------------------------------------------------------------

    @Operation(summary = "Dự trữ tồn kho cho đơn hàng (Reserved Stock)")
    @PatchMapping("/reserve/{productColorId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> reserveStock(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @RequestParam @NotNull(message = "Amount is required") @Min(value = 1, message = "Amount must be positive") int amount) {
        try {
            InventoryResponse response = inventoryService.reserveStock(productColorId, amount);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Dự trữ tồn kho thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Giải phóng tồn kho dự trữ (Release Reserved Stock)")
    @PatchMapping("/release/{productColorId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> releaseStock(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @RequestParam @NotNull(message = "Amount is required") @Min(value = 1, message = "Amount must be positive") int amount) {
        try {
            InventoryResponse response = inventoryService.releaseStock(productColorId, amount);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Giải phóng tồn kho dự trữ thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    // --------------------------------------------------------------------------
    // 3. TRUY VẤN TỒN KHO VÀ KIỂM TRA
    // --------------------------------------------------------------------------

    @Operation(summary = "Đếm tổng số lượng tồn kho vật lý (Physical Stock) của sản phẩm ProductColor")
    @GetMapping("/total-physical/{productColorId}")
    public ResponseEntity<ApiResponse<Integer>> getTotalStockByProduct(@PathVariable String productColorId) {
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho vật lý thành công")
                .data(inventoryService.getTotalStockByProductColorId(productColorId))
                .build());
    }

    @Operation(summary = "Đếm tổng số lượng tồn kho khả dụng (Available Stock) của sản phẩm ProductColor")
    @GetMapping("/total-available/{productColorId}")
    public ResponseEntity<ApiResponse<Integer>> getTotalAvailableStockByProductColorId(@PathVariable String productColorId) {
        return ResponseEntity.ok(ApiResponse.<Integer>builder()
                .status(200)
                .message("Lấy tổng tồn kho khả dụng thành công")
                .data(inventoryService.getTotalAvailableStockByProductColorId(productColorId))
                .build());
    }

    @Operation(summary = "Kiểm tra tồn kho cục bộ (Available Stock)")
    @GetMapping("/{productColorId}/{locationItemId}/check-stock")
    public ResponseEntity<ApiResponse<Boolean>> hasSufficientStock(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @PathVariable @NotBlank(message = "Location Item ID is required") String locationItemId,
            @RequestParam @NotNull(message = "Required quantity is required") @Min(value = 0, message = "Required quantity must be non-negative") int requiredQty) {
        boolean response = inventoryService.hasSufficientStock(productColorId, locationItemId, requiredQty);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho cục bộ thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Kiểm tra tồn kho toàn cục (Available Stock)")
    @GetMapping("/{productColorId}/check-global-stock")
    public ResponseEntity<ApiResponse<Boolean>> hasSufficientGlobalStock(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @RequestParam @NotNull(message = "Required quantity is required") @Min(value = 0, message = "Required quantity must be non-negative") int requiredQty) {
        boolean response = inventoryService.hasSufficientGlobalStock(productColorId, requiredQty);
        return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                .status(200)
                .message("Kiểm tra tồn kho toàn cục thành công")
                .data(response)
                .build());
    }

    // --------------------------------------------------------------------------
    // 4. TRUY VẤN DANH SÁCH & LỊCH SỬ
    // --------------------------------------------------------------------------

    @Operation(summary = "Lấy danh sách inventory theo product")
    @GetMapping("/productColorId/{productColorId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByProduct(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId) {
        List<InventoryResponse> response = inventoryService.getInventoryByProduct(productColorId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách inventory theo sản phẩm thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy danh sách inventory theo Zone")
    @GetMapping("/zone/{zoneId}")
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByZone(
            @PathVariable @NotBlank(message = "Zone ID is required") String zoneId) {
        List<InventoryResponse> response = inventoryService.getInventoryByZone(zoneId);
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy danh sách inventory theo Zone thành công")
                .data(response)
                .build());
    }

    @Operation(summary = "Lấy lịch sử giao dịch theo product và zone")
    @GetMapping("/transaction-history/{productColorId}/{zoneId}")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getTransactionHistory(
            @PathVariable @NotBlank(message = "Product ID is required") String productColorId,
            @PathVariable @NotBlank(message = "Zone ID is required") String zoneId) {
        try {
            List<InventoryTransactionResponse> response = inventoryService.getTransactionHistory(productColorId, zoneId);
            return ResponseEntity.ok(ApiResponse.<List<InventoryTransactionResponse>>builder()
                    .status(200)
                    .message("Lấy lịch sử giao dịch thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<List<InventoryTransactionResponse>>builder()
                            .status(404)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Lấy tất cả lịch sử giao dịch tồn kho")
    @GetMapping("/transactions")
    public ResponseEntity<ApiResponse<List<InventoryTransactionResponse>>> getAllTransactions() {
        List<InventoryTransactionResponse> transactions = inventoryService.getAllTransactions();
        return ResponseEntity.ok(ApiResponse.<List<InventoryTransactionResponse>>builder()
                .status(200)
                .message("Lấy tất cả lịch sử giao dịch tồn kho thành công")
                .data(transactions)
                .build());
    }

    @Operation(summary = "Lấy tất cả inventory")
    @GetMapping
    public ResponseEntity<ApiResponse<List<InventoryResponse>>> getAllInventory() {
        List<InventoryResponse> inventories = inventoryService.getAllInventory();
        return ResponseEntity.ok(ApiResponse.<List<InventoryResponse>>builder()
                .status(200)
                .message("Lấy tất cả inventory thành công")
                .data(inventories)
                .build());
    }

    @Operation(summary = "Lấy inventory theo ID")
    @GetMapping("/{inventoryId}")
    public ResponseEntity<ApiResponse<InventoryResponse>> getInventoryById(@PathVariable String inventoryId) {
        try {
            InventoryResponse response = inventoryService.getInventoryById(inventoryId);
            return ResponseEntity.ok(ApiResponse.<InventoryResponse>builder()
                    .status(200)
                    .message("Lấy inventory thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<InventoryResponse>builder()
                            .status(404)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Chuyển kho inventory")
    @PostMapping("/transfer")
    public ResponseEntity<ApiResponse<Void>> transferInventory(
            @RequestParam @NotBlank(message = "Product ID is required") String productColorId,
            @RequestParam @NotBlank(message = "Location Item ID is required") String locationItemId,
            @RequestParam @NotNull(message = "Quantity is required") @Min(value = 1, message = "Quantity must be positive") int quantity,
            @RequestParam @NotBlank(message = "Warehouse 1 ID is required") String warehouse1_Id,
            @RequestParam @NotBlank(message = "Warehouse 2 ID is required") String warehouse2_Id) {
        try {
            inventoryService.transferInventory(productColorId, locationItemId, quantity, warehouse1_Id, warehouse2_Id);
            return ResponseEntity.ok(ApiResponse.<Void>builder()
                    .status(200)
                    .message("Chuyển kho thành công")
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(ApiResponse.<Void>builder()
                            .status(400)
                            .message(e.getMessage())
                            .build());
        }
    }

    @Operation(summary = "Kiểm tra sức chứa của Zone")
    @GetMapping("/zone/{zoneId}/check-capacity")
    public ResponseEntity<ApiResponse<Boolean>> checkZoneCapacity(
            @PathVariable @NotBlank(message = "Zone ID is required") String zoneId) {
        try {
            boolean response = inventoryService.checkZoneCapacity(zoneId);
            return ResponseEntity.ok(ApiResponse.<Boolean>builder()
                    .status(200)
                    .message("Kiểm tra sức chứa Zone thành công")
                    .data(response)
                    .build());
        } catch (AppException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.<Boolean>builder()
                            .status(404)
                            .message(e.getMessage())
                            .build());
        }
    }
}
