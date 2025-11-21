package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.enums.EnumTypes;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

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
    private Long inventoryId;
}