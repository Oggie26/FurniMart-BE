package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.DeliveryAssignmentResponse;
import com.example.orderservice.response.DeliveryConfirmationResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "delivery-service")
public interface DeliveryClient {
    
    @GetMapping("/api/delivery/assignments/store/{storeId}")
    ApiResponse<List<DeliveryAssignmentResponse>> getDeliveryAssignmentsByStore(@PathVariable String storeId);
    
    @GetMapping("/api/delivery/assignments/order/{orderId}")
    ApiResponse<DeliveryAssignmentResponse> getDeliveryAssignmentByOrderId(@PathVariable Long orderId);

    @GetMapping("/api/delivery-confirmations/order/{orderId}")
    ApiResponse<DeliveryConfirmationResponse> getDeliveryConfirmation(@PathVariable Long orderId);
}

