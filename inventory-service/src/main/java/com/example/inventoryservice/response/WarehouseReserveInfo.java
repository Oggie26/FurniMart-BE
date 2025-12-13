package com.example.inventoryservice.response;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WarehouseReserveInfo {
    private String warehouseId;
    private String warehouseName;
    private int reservedQuantity;
    private boolean isAssignedWarehouse;
}

