package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.InventoryResponse;
import io.swagger.v3.oas.annotations.Parameter;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    @GetMapping("/api/inventories/product/{productId}")
    ApiResponse<List<InventoryResponse>> getInventoryByProduct(
            @PathVariable("productId") @NotBlank(message = "Product ID is required") String productId
    );

    @GetMapping("/api/inventories/{productColorId}/check-global-stock")
    ApiResponse<Boolean> hasSufficientGlobalStock(
            @PathVariable("productColorId") @NotBlank(message = "ProductColor ID is required") String productColorId,
            @RequestParam(name = "requiredQty") @NotNull(message = "Required quantity is required") @Min(value = 0, message = "Required quantity must be non-negative") int requiredQty
    );

    @GetMapping("/api/stock/total-available")
    ApiResponse<Integer> getAvailableStockByProductColorId(
            @RequestParam(name = "productColorId") @NotBlank(message = "ProductColor ID is required") String productColorId
    );
}
