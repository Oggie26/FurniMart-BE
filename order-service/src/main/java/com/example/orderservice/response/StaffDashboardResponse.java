package com.example.orderservice.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class StaffDashboardResponse {
    private Double personalRevenue;
    private Long createdOrdersCount;
    private Long pendingStoreOrdersCount;
}
