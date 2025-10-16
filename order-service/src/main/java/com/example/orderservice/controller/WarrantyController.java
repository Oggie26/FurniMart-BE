package com.example.orderservice.controller;

import com.example.orderservice.request.WarrantyClaimRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.WarrantyClaimResponse;
import com.example.orderservice.response.WarrantyResponse;
import com.example.orderservice.service.inteface.WarrantyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/warranties")
@Tag(name = "Warranty Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WarrantyController {

    private final WarrantyService warrantyService;

    // ========== CUSTOMER ENDPOINTS ==========

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get warranties by customer ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<WarrantyResponse>> getWarrantiesByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<WarrantyResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Warranties retrieved successfully")
                .data(warrantyService.getWarrantiesByCustomer(customerId))
                .build();
    }

    @GetMapping("/customer/{customerId}/active")
    @Operation(summary = "Get active warranties by customer ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<WarrantyResponse>> getActiveWarrantiesByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<WarrantyResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Active warranties retrieved successfully")
                .data(warrantyService.getActiveWarrantiesByCustomer(customerId))
                .build();
    }

    @GetMapping("/{warrantyId}")
    @Operation(summary = "Get warranty by ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<WarrantyResponse> getWarrantyById(@PathVariable Long warrantyId) {
        return ApiResponse.<WarrantyResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Warranty retrieved successfully")
                .data(warrantyService.getWarrantyById(warrantyId))
                .build();
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get warranties by order ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<WarrantyResponse>> getWarrantiesByOrder(@PathVariable Long orderId) {
        return ApiResponse.<List<WarrantyResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Warranties retrieved successfully")
                .data(warrantyService.getWarrantiesByOrder(orderId))
                .build();
    }

    @PostMapping("/claims")
    @Operation(summary = "Create warranty claim")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<WarrantyClaimResponse> createWarrantyClaim(@Valid @RequestBody WarrantyClaimRequest request) {
        return ApiResponse.<WarrantyClaimResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Warranty claim created successfully")
                .data(warrantyService.createWarrantyClaim(request))
                .build();
    }

    @GetMapping("/claims/customer/{customerId}")
    @Operation(summary = "Get warranty claims by customer ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<WarrantyClaimResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Warranty claims retrieved successfully")
                .data(warrantyService.getWarrantyClaimsByCustomer(customerId))
                .build();
    }

    @GetMapping("/claims/warranty/{warrantyId}")
    @Operation(summary = "Get warranty claims by warranty ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByWarranty(@PathVariable Long warrantyId) {
        return ApiResponse.<List<WarrantyClaimResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Warranty claims retrieved successfully")
                .data(warrantyService.getWarrantyClaimsByWarranty(warrantyId))
                .build();
    }

    // ========== ADMIN ENDPOINTS ==========

    @GetMapping("/claims")
    @Operation(summary = "Get all warranty claims (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WarrantyClaimResponse>> getAllWarrantyClaims() {
        return ApiResponse.<List<WarrantyClaimResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("All warranty claims retrieved successfully")
                .data(warrantyService.getAllWarrantyClaims())
                .build();
    }

    @GetMapping("/claims/status/{status}")
    @Operation(summary = "Get warranty claims by status (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<WarrantyClaimResponse>> getWarrantyClaimsByStatus(@PathVariable String status) {
        return ApiResponse.<List<WarrantyClaimResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Warranty claims retrieved successfully")
                .data(warrantyService.getWarrantyClaimsByStatus(status))
                .build();
    }

    @PutMapping("/claims/{claimId}/status")
    @Operation(summary = "Update warranty claim status (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WarrantyClaimResponse> updateWarrantyClaimStatus(
            @PathVariable Long claimId,
            @RequestParam String status,
            @RequestParam(required = false) String adminResponse,
            @RequestParam(required = false) String resolutionNotes) {
        return ApiResponse.<WarrantyClaimResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Warranty claim status updated successfully")
                .data(warrantyService.updateWarrantyClaimStatus(claimId, status, adminResponse, resolutionNotes))
                .build();
    }
}
