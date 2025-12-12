package com.example.aiservice.feign;

import com.example.aiservice.response.ApiResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service")
public interface InventoryClient {

    /**
     * Kiểm tra stock tại store cụ thể
     */
    @GetMapping("/api/inventories/stock/check-at-store")
    ApiResponse<Boolean> checkStockAtStore(
            @RequestParam("productColorId") String productColorId,
            @RequestParam("storeId") String storeId,
            @RequestParam("quantity") Integer quantity);
}
