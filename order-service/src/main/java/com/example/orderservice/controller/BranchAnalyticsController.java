package com.example.orderservice.controller;

import com.example.orderservice.response.*;
import com.example.orderservice.service.BranchAnalyticsService;
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
@RequestMapping("/api/v1/branch")
@RequiredArgsConstructor
@Tag(name = "Branch Analytics Controller", description = "APIs for branch analytics and statistics")
@SecurityRequirement(name = "bearerAuth")
public class BranchAnalyticsController {

    private final BranchAnalyticsService branchAnalyticsService;

    @GetMapping("/stats/daily")
    @Operation(
            summary = "Get daily statistics",
            description = "Returns real-time daily statistics for the branch including orders, revenue, and customer metrics"
    )
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<BranchDailyStatsResponse> getDailyStats(
            @RequestParam String storeId) {
        BranchDailyStatsResponse data = branchAnalyticsService.getDailyStats(storeId);
        
        return ApiResponse.<BranchDailyStatsResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Daily statistics retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/inventory/summary")
    @Operation(
            summary = "Get inventory summary",
            description = "Returns a quick summary table of inventory status including total products, low stock, out of stock, and in stock items"
    )
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<InventorySummaryResponse> getInventorySummary(
            @RequestParam String storeId) {
        InventorySummaryResponse data = branchAnalyticsService.getInventorySummary(storeId);
        
        return ApiResponse.<InventorySummaryResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Inventory summary retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/trend")
    @Operation(
            summary = "Get activity trend",
            description = "Returns revenue and order count data for a short time period (default: 7 days). Use days parameter to specify the period."
    )
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<ActivityTrendResponse> getActivityTrend(
            @RequestParam String storeId,
            @RequestParam(defaultValue = "7") int days) {
        ActivityTrendResponse data = branchAnalyticsService.getActivityTrend(storeId, days);
        
        return ApiResponse.<ActivityTrendResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Activity trend retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/order-status")
    @Operation(
            summary = "Get order status breakdown",
            description = "Returns total number of orders grouped by current status with percentages"
    )
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<OrderStatusBreakdownResponse> getOrderStatusBreakdown(
            @RequestParam String storeId) {
        OrderStatusBreakdownResponse data = branchAnalyticsService.getOrderStatusBreakdown(storeId);
        
        return ApiResponse.<OrderStatusBreakdownResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Order status breakdown retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/analytics/top-products")
    @Operation(
            summary = "Get top products by branch",
            description = "Returns top products for the current branch. Use limit parameter to specify number of products (default: 10)"
    )
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('STAFF')")
    public ApiResponse<List<TopProductResponse>> getTopProducts(
            @RequestParam String storeId,
            @RequestParam(defaultValue = "10") int limit) {
        List<TopProductResponse> data = branchAnalyticsService.getTopProducts(storeId, limit);
        
        return ApiResponse.<List<TopProductResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Top products retrieved successfully")
                .data(data)
                .build();
    }
}

