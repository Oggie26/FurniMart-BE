package com.example.deliveryservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreBranchInfoResponse {
    private StoreResponse store;
    private List<ProductStockInfo> productStockInfo;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductStockInfo {
        private String productColorId;
        private String productName;
        private Integer availableStock;
        private Boolean inStock;
    }
}

