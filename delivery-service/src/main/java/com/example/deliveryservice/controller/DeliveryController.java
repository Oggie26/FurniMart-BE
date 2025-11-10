package com.example.deliveryservice.controller;

import com.example.deliveryservice.request.AssignOrderRequest;
import com.example.deliveryservice.request.PrepareProductsRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DeliveryAssignmentResponse;
import com.example.deliveryservice.response.DeliveryProgressResponse;
import com.example.deliveryservice.response.StoreBranchInfoResponse;
import com.example.deliveryservice.service.inteface.DeliveryService;
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
@RequestMapping("/api/delivery")
@Tag(name = "Delivery Controller", description = "APIs for managing delivery assignments, store branch info, invoices, and delivery progress")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeliveryController {

    private final DeliveryService deliveryService;

    // ========== GUEST ENDPOINTS (Public) ==========

    @GetMapping("/stores/{storeId}/branch-info")
    @Operation(summary = "Get store branch information with stock availability (Public - Guests can view)")
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<StoreBranchInfoResponse> getStoreBranchInfo(@PathVariable String storeId) {
        return ApiResponse.<StoreBranchInfoResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Store branch information retrieved successfully")
                .data(deliveryService.getStoreBranchInfo(storeId))
                .build();
    }

    // ========== STAFF ENDPOINTS ==========

    @PostMapping("/assign")
    @Operation(summary = "Assign order to delivery staff (Staff/Branch Manager only)")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('STAFF') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryAssignmentResponse> assignOrderToDelivery(@Valid @RequestBody AssignOrderRequest request) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Order assigned to delivery successfully")
                .data(deliveryService.assignOrderToDelivery(request))
                .build();
    }

    @PostMapping("/generate-invoice/{orderId}")
    @Operation(summary = "Generate invoice for order (Staff only)")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<DeliveryAssignmentResponse> generateInvoice(@PathVariable Long orderId) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Invoice generated successfully")
                .data(deliveryService.generateInvoice(orderId))
                .build();
    }

    @PostMapping("/prepare-products")
    @Operation(summary = "Prepare products for delivery (Staff only)")
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STAFF')")
    public ApiResponse<DeliveryAssignmentResponse> prepareProducts(@Valid @RequestBody PrepareProductsRequest request) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Products prepared successfully")
                .data(deliveryService.prepareProducts(request))
                .build();
    }

    @GetMapping("/assignments/store/{storeId}")
    @Operation(summary = "Get delivery assignments by store (Staff/Branch Manager only)")
    @PreAuthorize("hasRole('STAFF') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<DeliveryAssignmentResponse>> getDeliveryAssignmentsByStore(@PathVariable String storeId) {
        return ApiResponse.<List<DeliveryAssignmentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery assignments retrieved successfully")
                .data(deliveryService.getDeliveryAssignmentsByStore(storeId))
                .build();
    }

    @GetMapping("/assignments/order/{orderId}")
    @Operation(summary = "Get delivery assignment by order ID (Staff/Branch Manager only)")
    @PreAuthorize("hasRole('STAFF') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryAssignmentResponse> getDeliveryAssignmentByOrderId(@PathVariable Long orderId) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery assignment retrieved successfully")
                .data(deliveryService.getDeliveryAssignmentByOrderId(orderId))
                .build();
    }

    // ========== BRANCH MANAGER ENDPOINTS ==========

    @GetMapping("/progress/store/{storeId}")
    @Operation(summary = "Monitor delivery progress within branch (Branch Manager only)")
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryProgressResponse> getDeliveryProgressByStore(@PathVariable String storeId) {
        return ApiResponse.<DeliveryProgressResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery progress retrieved successfully")
                .data(deliveryService.getDeliveryProgressByStore(storeId))
                .build();
    }

    @PutMapping("/assignments/{assignmentId}/status")
    @Operation(summary = "Update delivery status (Branch Manager/Delivery Staff only)")
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('DELIVERY')")
    public ApiResponse<DeliveryAssignmentResponse> updateDeliveryStatus(
            @PathVariable Long assignmentId,
            @RequestParam String status) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery status updated successfully")
                .data(deliveryService.updateDeliveryStatus(assignmentId, status))
                .build();
    }

    // ========== DELIVERY STAFF ENDPOINTS ==========

    @GetMapping("/assignments/staff/{deliveryStaffId}")
    @Operation(summary = "Get delivery assignments by delivery staff (Delivery Staff only)")
    @PreAuthorize("hasRole('DELIVERY')")
    public ApiResponse<List<DeliveryAssignmentResponse>> getDeliveryAssignmentsByStaff(@PathVariable String deliveryStaffId) {
        return ApiResponse.<List<DeliveryAssignmentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery assignments retrieved successfully")
                .data(deliveryService.getDeliveryAssignmentsByStaff(deliveryStaffId))
                .build();
    }
}

