package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopProductResponse {
    private String productColorId;
    private String productName;
    private String colorName;
    private Long totalQuantitySold;
    private Double totalRevenue;
}

