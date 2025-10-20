package com.example.deliveryservice.feign;

import com.example.deliveryservice.enums.EnumProcessOrder;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.OrderResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface OrderClient {

    @GetMapping("/api/orders/{id}")
    ResponseEntity<ApiResponse<OrderResponse>> getOrderById(@PathVariable("id") Long id);

    @PutMapping("/api/orders/status/{id}")
    ResponseEntity<ApiResponse<OrderResponse>> updateOrderStatus(@PathVariable("id") Long id,
                                                                 @RequestParam("status") EnumProcessOrder status);
}


