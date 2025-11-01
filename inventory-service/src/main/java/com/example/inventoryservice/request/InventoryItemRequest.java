package com.example.inventoryservice.request;

import com.example.inventoryservice.entity.Inventory;
import com.example.inventoryservice.entity.LocationItem;
import com.example.inventoryservice.enums.EnumPurpose;
import com.example.inventoryservice.enums.EnumStatus;
import com.example.inventoryservice.enums.EnumTypes;
import jakarta.persistence.*;
import lombok.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryItemRequest {
    private Integer quantity;
    private String productColorId;
    private String locationItemId;
    private Long inventoryId;
    private String warehouseId;
}
