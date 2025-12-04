package com.example.orderservice.controller;

import com.example.orderservice.response.*;
import com.example.orderservice.service.AdminAnalyticsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Analytics Controller", description = "APIs for admin analytics and statistics")
@SecurityRequirement(name = "bearerAuth")
public class AdminAnalyticsController {

    private final AdminAnalyticsService adminAnalyticsService;

    @GetMapping("/stats/overview")
    @Operation(
            summary = "Get overview statistics",
            description = "Returns 4 main statistics for Top Cards: total revenue, total orders, total active stores, and total users"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<OverviewStatsResponse> getOverviewStats() {
        OverviewStatsResponse data = adminAnalyticsService.getOverviewStats();
        
        return ApiResponse.<OverviewStatsResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Overview statistics retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/revenue-by-branch")
    @Operation(
            summary = "Get revenue by branch",
            description = "Returns data for bar chart comparing revenue across different branches"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<RevenueByBranchResponse> getRevenueByBranch() {
        RevenueByBranchResponse data = adminAnalyticsService.getRevenueByBranch();
        
        return ApiResponse.<RevenueByBranchResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Revenue by branch retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/top-products")
    @Operation(
            summary = "Get top products",
            description = "Returns top 5-10 products by sales. Use limit parameter to specify number of products (default: 10)"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<TopProductResponse>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        List<TopProductResponse> data = adminAnalyticsService.getTopProducts(limit);
        
        return ApiResponse.<List<TopProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Top products retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/delivery-performance")
    @Operation(
            summary = "Get delivery performance",
            description = "Returns delivery performance statistics with ratios for pie chart showing order status distribution"
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<DeliveryPerformanceResponse> getDeliveryPerformance() {
        DeliveryPerformanceResponse data = adminAnalyticsService.getDeliveryPerformance();
        
        return ApiResponse.<DeliveryPerformanceResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery performance retrieved successfully")
                .data(data)
                .build();
    }
}

