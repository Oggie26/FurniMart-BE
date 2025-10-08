package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.InventoryResponse;
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
    ResponseEntity<ApiResponse<List<InventoryResponse>>> getInventoryByProduct(@PathVariable @NotBlank(message = "Product ID is required") String productId);

    @GetMapping("/api/inventories/{productColorId}/check-global-stock")
    ResponseEntity<ApiResponse<Boolean>> hasSufficientGlobalStock  (@PathVariable @NotBlank(message = "Product ID is required") String productColorId,
    @RequestParam @NotNull(message = "Required quantity is required") @Min(value = 0, message = "Required quantity must be non-negative") int requiredQty);
}
