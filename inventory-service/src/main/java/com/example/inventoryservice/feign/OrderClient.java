package com.example.inventoryservice.feign;

import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.OrderResponse;
import com.example.inventoryservice.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", contextId = "orderClientForInventory")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    ApiResponse<OrderResponse> getOderById(@PathVariable("id") Long id);


}
