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
public class ManagerDashboardResponse {
    private Double branchRevenue;
    private Long pendingOrdersCount;
    private Long shippingOrdersCount;
    private List<LowStockProductResponse> lowStockProducts;
    private List<OrderForShipperResponse> ordersForShipper;
}

