package com.example.orderservice.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarrantyClaimRequest {
    
    @NotNull(message = "Order ID cannot be null")
    private Long orderId;

    private Long addressId;

    @NotNull(message = "Items cannot be null")
    private List<WarrantyClaimItemRequest> items;
}

