package com.example.inventoryservice.response;

import com.example.inventoryservice.entity.Inventory;
import lombok.Data;

@Data
public class ReserveStockResponse {
    private Inventory inventory;
    private int quantityReserved;
    private int quantityMissing; // 0 nếu đủ
}
