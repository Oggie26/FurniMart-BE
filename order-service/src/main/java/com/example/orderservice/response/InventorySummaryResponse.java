package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InventorySummaryResponse {
    private Long totalProducts;
    private Long lowStockProducts;
    private Long outOfStockProducts;
    private Long inStockProducts;
    private List<InventoryItemSummary> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InventoryItemSummary {
        private String productId;
        private String productName;
        private String colorName;
        private Integer currentStock;
        private Integer minStock;
        private String status; // IN_STOCK, LOW_STOCK, OUT_OF_STOCK
    }
}

