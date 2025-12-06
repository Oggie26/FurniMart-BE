package com.example.inventoryservice.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class ReserveStockResponse {
    private List<InventoryResponse> inventory;
    private int quantityReserved;
    private int quantityMissing; // 0 nếu đủ
}
