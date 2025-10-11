package com.example.notificationservice.feign;

import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "order-service", contextId = "orderClient")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    ApiResponse<OrderResponse> getOrderById(@PathVariable Long id);
}
