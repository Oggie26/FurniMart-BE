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
public class AdminDashboardResponse {
    private Double totalRevenue;
    private Long totalActiveStores;
    private Long totalUsers;
    private List<TopProductResponse> topProducts;
    private List<RevenueChartData> revenueChart;
    private RevenueByBranchResponse revenueByBranch;
    private DeliveryPerformanceResponse deliveryPerformance;
}

