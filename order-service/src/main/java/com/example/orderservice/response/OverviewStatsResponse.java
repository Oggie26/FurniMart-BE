package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OverviewStatsResponse {
    private Double totalRevenue;
    private Long totalOrders;
    private Long totalActiveStores;
    private Long totalUsers;
}

