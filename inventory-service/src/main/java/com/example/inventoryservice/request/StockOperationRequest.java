package com.example.inventoryservice.request;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StockOperationRequest {
    private String productColorId;
    private String locationItemId; // dùng cho import/export
    private int quantity;
    private String warehouseId;    // warehouse nhập/xuất
    private String employeeId;

    // Dành riêng cho transfer
    private String fromWarehouseId;
    private String toWarehouseId;
}
