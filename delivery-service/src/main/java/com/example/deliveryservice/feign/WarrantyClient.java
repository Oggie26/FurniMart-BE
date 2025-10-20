package com.example.deliveryservice.feign;

import com.example.deliveryservice.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "order-service")
public interface WarrantyClient {

    @PostMapping("/internal/warranties/generate")
    ResponseEntity<ApiResponse<Void>> generateWarranties(@RequestParam("orderId") Long orderId);
}


