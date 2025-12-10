package com.example.inventoryservice.feign;

import com.example.inventoryservice.enums.EnumProcessOrder;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service", contextId = "orderClientForInventory")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    ApiResponse<OrderResponse> getOderById(@PathVariable("id") Long id);

    @PutMapping("/api/orders/status/{id}")
    ApiResponse<OrderResponse> updateOrderStatus(@PathVariable("id") Long id, @RequestParam("status") EnumProcessOrder status);


}
