package com.example.productservice.feign;

import com.example.productservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "inventory-service", url = "http://localhost:8084/api/inventories")
public interface InventoryClient {

    @GetMapping("/stock/total-available")
    ApiResponse<Integer> getAvailableStockByProductColorId(@RequestParam("productColorId") String productColorId);
}
