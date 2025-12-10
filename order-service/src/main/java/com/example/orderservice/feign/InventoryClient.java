package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.InventoryResponse;
import com.example.orderservice.response.LowStockProductResponse;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

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
        //
        // @PostMapping("/api/inventories/reserve/{orderId}")
        // ApiResponse<ReserveStockResponse> reserveStock(
        // @PathVariable Long orderId,
        // @RequestParam("productColorId") @NotBlank String productColorId,
        // @RequestParam("quantity") @Min(1) int quantity)

}
