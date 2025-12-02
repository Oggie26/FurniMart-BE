package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Inventory;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ReserveStockResponse {
    private Inventory inventory;
    private int quantityReserved;
    private int quantityMissing; // 0 nếu đủ
}
