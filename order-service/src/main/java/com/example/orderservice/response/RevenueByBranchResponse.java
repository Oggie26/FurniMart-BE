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
public class RevenueByBranchResponse {
    private List<BranchRevenueData> branches;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BranchRevenueData {
        private String branchId;
        private String branchName;
        private Double revenue;
        private Long orderCount;
    }
}

