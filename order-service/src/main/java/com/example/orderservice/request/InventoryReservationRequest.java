package com.example.orderservice.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class InventoryReservationRequest {
    private Long orderId;
    private String productColorId;
    private int quantity;
}
