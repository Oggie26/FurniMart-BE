package com.example.deliveryservice.controller;

import com.example.deliveryservice.request.AssignOrderRequest;
import com.example.deliveryservice.request.PrepareProductsRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DeliveryAssignmentResponse;
import com.example.deliveryservice.response.DeliveryProgressResponse;
import com.example.deliveryservice.response.StoreBranchInfoResponse;
import com.example.deliveryservice.service.inteface.DeliveryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
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
    @Operation(
            summary = "Get store branch information with stock availability",
            description = "Retrieve detailed store information including warehouse details and stock availability. This is a public API that does not require authentication."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Store information retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Store not found with storeId: {storeId}")
    })
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
    @Operation(
            summary = "Assign order to delivery staff",
            description = "Assign an order to a delivery staff member. Only STAFF and BRANCH_MANAGER roles can use this API. " +
                    "The order will be assigned to the specified deliveryStaffId (required), with estimated delivery date and notes (if provided). " +
                    "After successful assignment, the assignment status will be ASSIGNED."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Order assigned to delivery staff successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Order already assigned - Assignment ID and Status will be returned in the message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found with orderId: {orderId} OR Store not found with storeId: {storeId}"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only STAFF and BRANCH_MANAGER roles are allowed")
    })
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
    @Operation(
            summary = "Generate invoice for order",
            description = "Generate an invoice for an order. Only BRANCH_MANAGER role can use this API. " +
                    "Each order can only have an invoice generated once. After successful generation, invoiceGenerated will be set to true."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Invoice generated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invoice already generated for this order"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery assignment not found with orderId: {orderId}"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only BRANCH_MANAGER role is allowed")
    })
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryAssignmentResponse> generateInvoice(@PathVariable Long orderId) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Invoice generated successfully")
                .data(deliveryService.generateInvoice(orderId))
                .build();
    }

    @PostMapping("/prepare-products")
    @Operation(
            summary = "Prepare products for delivery",
            description = "Prepare products for delivery. STAFF and BRANCH_MANAGER roles can use this API. " +
                    "The system will check stock availability for each product in the order. If stock is insufficient, it will return INSUFFICIENT_STOCK error. " +
                    "After successful preparation, productsPrepared will be set to true and status will change to PREPARING. " +
                    "Manager can use this to control inventory shortages and request stock from other warehouses."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Products prepared successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Products already prepared OR Insufficient stock - Details of missing products will be returned in the message"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery assignment not found with orderId: {orderId} OR Order not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only STAFF and BRANCH_MANAGER roles are allowed")
    })
    @ResponseStatus(HttpStatus.OK)
    @PreAuthorize("hasRole('STAFF') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryAssignmentResponse> prepareProducts(@Valid @RequestBody PrepareProductsRequest request) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Products prepared successfully")
                .data(deliveryService.prepareProducts(request))
                .build();
    }

    @GetMapping("/assignments/store/{storeId}")
    @Operation(
            summary = "Get delivery assignments by store",
            description = "Retrieve all delivery assignments for a specific store. Only STAFF and BRANCH_MANAGER roles can use this API."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery assignments retrieved successfully (may return empty list if no assignments exist)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only STAFF and BRANCH_MANAGER roles are allowed")
    })
    @PreAuthorize("hasRole('STAFF') or hasRole('BRANCH_MANAGER')")
    public ApiResponse<List<DeliveryAssignmentResponse>> getDeliveryAssignmentsByStore(@PathVariable String storeId) {
        return ApiResponse.<List<DeliveryAssignmentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery assignments retrieved successfully")
                .data(deliveryService.getDeliveryAssignmentsByStore(storeId))
                .build();
    }

    @GetMapping("/assignments/order/{orderId}")
    @Operation(
            summary = "Get delivery assignment by order ID",
            description = "Retrieve delivery assignment information for a specific order. Only STAFF and BRANCH_MANAGER roles can use this API."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery assignment retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery assignment not found with orderId: {orderId}"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only STAFF and BRANCH_MANAGER roles are allowed")
    })
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
    @Operation(
            summary = "Monitor delivery progress within branch",
            description = "Monitor delivery progress for a store. Only BRANCH_MANAGER role can use this API. " +
                    "Returns statistics of order counts by status: ASSIGNED, PREPARING, READY, IN_TRANSIT, DELIVERED."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery progress retrieved successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Store not found with storeId: {storeId}"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only BRANCH_MANAGER role is allowed")
    })
    @PreAuthorize("hasRole('BRANCH_MANAGER')")
    public ApiResponse<DeliveryProgressResponse> getDeliveryProgressByStore(@PathVariable String storeId) {
        return ApiResponse.<DeliveryProgressResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery progress retrieved successfully")
                .data(deliveryService.getDeliveryProgressByStore(storeId))
                .build();
    }

    @PutMapping("/assignments/{assignmentId}/status")
    @Operation(
            summary = "Update delivery status",
            description = "Update delivery status. Only BRANCH_MANAGER and DELIVERY roles can use this API. " +
                    "Valid status values: ASSIGNED (Assigned), PREPARING (Preparing), READY (Ready for delivery), " +
                    "IN_TRANSIT (In transit), DELIVERED (Delivered), CANCELLED (Cancelled). " +
                    "Status must be sent as a string (e.g., 'ASSIGNED', 'IN_TRANSIT')."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery status updated successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid Status - Status must be one of: ASSIGNED, PREPARING, READY, IN_TRANSIT, DELIVERED, CANCELLED"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery assignment not found with assignmentId: {assignmentId}"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only BRANCH_MANAGER and DELIVERY roles are allowed")
    })
    @PreAuthorize("hasRole('BRANCH_MANAGER') or hasRole('DELIVERY')")
    public ApiResponse<DeliveryAssignmentResponse> updateDeliveryStatus(
            @Parameter(description = "Delivery assignment ID", required = true, example = "1")
            @PathVariable Long assignmentId,
            @Parameter(
                    description = "Delivery status. Valid values: ASSIGNED, PREPARING, READY, IN_TRANSIT, DELIVERED, CANCELLED",
                    required = true,
                    schema = @Schema(
                            type = "string",
                            allowableValues = {"ASSIGNED", "PREPARING", "READY", "IN_TRANSIT", "DELIVERED", "CANCELLED"},
                            example = "IN_TRANSIT"
                    )
            )
            @RequestParam String status) {
        return ApiResponse.<DeliveryAssignmentResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery status updated successfully")
                .data(deliveryService.updateDeliveryStatus(assignmentId, status))
                .build();
    }

    // ========== DELIVERY STAFF ENDPOINTS ==========

    @GetMapping("/assignments/staff/{deliveryStaffId}")
    @Operation(
            summary = "Get delivery assignments by delivery staff",
            description = "Retrieve all delivery assignments for a specific delivery staff member. Only DELIVERY role can use this API."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery assignments retrieved successfully (may return empty list if no assignments exist)"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only DELIVERY role is allowed")
    })
    @PreAuthorize("hasRole('DELIVERY')")
    public ApiResponse<List<DeliveryAssignmentResponse>> getDeliveryAssignmentsByStaff(@PathVariable String deliveryStaffId) {
        return ApiResponse.<List<DeliveryAssignmentResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery assignments retrieved successfully")
                .data(deliveryService.getDeliveryAssignmentsByStaff(deliveryStaffId))
                .build();
    }
}

