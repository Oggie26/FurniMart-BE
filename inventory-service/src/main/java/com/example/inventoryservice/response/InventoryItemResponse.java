package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.LocationItem;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventoryItemResponse {
    private Long id;
    private Integer quantity;
    private String productColorId;
    private String productName;
    private Integer reservedQuantity;
    private LocationItem locationItem;
    private String locationId;
    private Long inventoryId;
}