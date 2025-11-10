package com.example.deliveryservice.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PrepareProductsRequest {
    @NotNull(message = "Order ID is required")
    private Long orderId;
    
    private String notes;
}

