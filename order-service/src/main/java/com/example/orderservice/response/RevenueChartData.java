package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueChartData {
    private String date; // Format: "yyyy-MM-dd" or "yyyy-MM"
    private Double revenue;
    private Long orderCount;
}

