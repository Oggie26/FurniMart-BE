package com.example.orderservice.controller;

import com.example.orderservice.response.AdminDashboardResponse;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.ManagerDashboardResponse;
import com.example.orderservice.service.DashboardService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
@Tag(name = "Dashboard Controller", description = "APIs for Admin and Manager dashboards")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/admin")
    @Operation(summary = "Get Admin Dashboard Data", description = "Returns total revenue, active stores, users, top products, and revenue chart")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<AdminDashboardResponse> getAdminDashboard() {
        AdminDashboardResponse data = dashboardService.getAdminDashboard();
        
        return ApiResponse.<AdminDashboardResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Admin dashboard data retrieved successfully")
                .data(data)
                .build();
    }

    @GetMapping("/manager")
    @Operation(summary = "Get Manager Dashboard Data", description = "Returns branch revenue, pending/shipping orders, low stock products, and orders for shipper")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<ManagerDashboardResponse> getManagerDashboard(
            @RequestParam String storeId) {
        ManagerDashboardResponse data = dashboardService.getManagerDashboard(storeId);
        
        return ApiResponse.<ManagerDashboardResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Manager dashboard data retrieved successfully")
                .data(data)
                .build();
    }
}

