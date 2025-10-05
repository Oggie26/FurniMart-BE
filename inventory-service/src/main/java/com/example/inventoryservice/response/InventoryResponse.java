package com.example.inventoryservice.response;

import com.example.inventoryservice.enums.EnumStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryResponse {
    private String id;
    private String productColorId;
    private String locationItemId;
    private int quantity;
    private int min_quantity;
    private int max_quantity;
    private EnumStatus status;
}