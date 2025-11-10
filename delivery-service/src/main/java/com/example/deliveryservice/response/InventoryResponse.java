package com.example.deliveryservice.response;

import com.example.deliveryservice.enums.EnumStatus;
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
    private Integer quantity;
    private Integer reserved_quantity;
    private Integer available_quantity;
    private Integer min_quantity;
    private Integer max_quantity;
    private EnumStatus status;
}

