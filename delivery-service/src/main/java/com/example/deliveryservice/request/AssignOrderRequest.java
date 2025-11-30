package com.example.deliveryservice.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AssignOrderRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    @NotNull(message = "Store ID is required")
    private String storeId;
    
    @NotNull(message = "Delivery staff ID is required")
    private String deliveryStaffId;
    
    private LocalDateTime estimatedDeliveryDate;
    
    private String notes;
}

