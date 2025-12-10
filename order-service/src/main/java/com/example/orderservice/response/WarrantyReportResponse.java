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
public class WarrantyReportResponse {
    private Long totalClaims;
    private Long pendingClaims;
    private Long resolvedClaims;
    private Long exchangeCount;
    private Long returnCount;
    private Long repairCount;
    private Long rejectedCount;
    private Double totalRepairCost; // Admin/Manager only
    private Double totalRefundAmount;
    private List<WarrantyClaimResponse> recentClaims;
}
