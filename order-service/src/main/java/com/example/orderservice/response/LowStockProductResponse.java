package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockProductResponse {
    private String productColorId;
    private String productName;
    private String colorName;
    private Integer currentStock;
    private Integer threshold;
    private String warehouseName;
    private String locationCode;
}
