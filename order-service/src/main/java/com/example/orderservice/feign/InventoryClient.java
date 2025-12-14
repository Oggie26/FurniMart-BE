package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.InventoryResponse;
import com.example.orderservice.response.LowStockProductResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import com.example.orderservice.request.InventoryReservationRequest;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

        @GetMapping("/api/inventories/product/{productId}")
        ApiResponse<List<InventoryResponse>> getInventoryByProduct(
                        @PathVariable("productId") @NotBlank(message = "Product ID is required") String productId);

        @GetMapping("/api/inventories/stock/check-global")
        ApiResponse<Boolean> hasSufficientGlobalStock(
                        @RequestParam("productColorId") @NotBlank(message = "ProductColor ID is required") String productColorId,
                        @RequestParam(name = "requiredQty") @NotNull(message = "Required quantity is required") @Min(value = 0, message = "Required quantity must be non-negative") int requiredQty);

        @GetMapping("/api/inventories/stock/total-available")
        ApiResponse<Integer> getAvailableStockByProductColorId(
                        @RequestParam(name = "productColorId") @NotBlank(message = "ProductColor ID is required") String productColorId);

        @GetMapping("/api/inventories/stock/low-stock")
        ApiResponse<List<LowStockProductResponse>> getLowStockProducts(
                        @RequestParam(name = "storeId", required = false) String storeId,
                        @RequestParam(name = "threshold", defaultValue = "10") @Min(1) int threshold);

        @PostMapping("/api/inventories/stock/restore")
        ApiResponse<Void> restoreStock(
                        @RequestParam("productColorId") @NotBlank String productColorId,
                        @RequestParam("quantity") @Min(1) int quantity);

        @DeleteMapping("/api/inventories/rollback/{orderId}")
        ApiResponse<Void> rollbackInventory(@PathVariable Long orderId);

        @PostMapping("/api/inventories/reserve")
        ApiResponse<Void> reserveInventory(@RequestBody List<InventoryReservationRequest> requests);

        @PostMapping("/api/inventories/commit")
        ApiResponse<Void> commitInventory(@RequestParam Long orderId);

        @PostMapping("/api/inventories/release")
        ApiResponse<Void> releaseInventory(@RequestParam Long orderId);

        /**
         * Kiểm tra stock tại 1 store cụ thể
         */
        @GetMapping("/api/inventories/stock/check-at-store")
        ApiResponse<Boolean> checkStockAtStore(
                        @RequestParam("productColorId") String productColorId,
                        @RequestParam("storeId") String storeId,
                        @RequestParam("quantity") Integer quantity);

}
