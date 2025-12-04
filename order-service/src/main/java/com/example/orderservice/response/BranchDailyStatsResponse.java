package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchDailyStatsResponse {
    private Long totalOrdersToday;
    private Long pendingOrders;
    private Long processingOrders;
    private Long completedOrders;
    private Double revenueToday;
    private Long newCustomersToday;
}

