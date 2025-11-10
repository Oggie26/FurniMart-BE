package com.example.deliveryservice.feign;

import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.InventoryResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "inventory-service", contextId = "inventoryClient")
public interface InventoryClient {

    @GetMapping("/api/inventory/product/{productColorId}")
    ApiResponse<List<InventoryResponse>> getInventoryByProduct(@PathVariable("productColorId") String productColorId);

    @GetMapping("/api/inventory/total-available/{productColorId}")
    ApiResponse<Integer> getTotalAvailableStock(@PathVariable("productColorId") String productColorId);
}

