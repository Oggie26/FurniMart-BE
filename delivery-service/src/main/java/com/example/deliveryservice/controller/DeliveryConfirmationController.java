package com.example.deliveryservice.controller;

import com.example.deliveryservice.request.DeliveryConfirmationRequest;
import com.example.deliveryservice.request.IncidentReportRequest;
import com.example.deliveryservice.request.QRCodeScanRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DeliveryConfirmationResponse;
import com.example.deliveryservice.service.inteface.DeliveryConfirmationService;
import io.swagger.v3.oas.annotations.Operation;
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
@RequestMapping("/api/delivery-confirmations")
@Tag(name = "Delivery Confirmation Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeliveryConfirmationController {

        private final DeliveryConfirmationService deliveryConfirmationService;

        // ========== DELIVERY STAFF ENDPOINTS ==========

        @PostMapping
        @Operation(summary = "Create delivery confirmation with photos", description = "Create a delivery confirmation with photos. Only DELIVERY role can use this API. "
                        +
                        "This API will automatically retrieve the QR code from Order Service and create a delivery confirmation with status DELIVERED. "
                        +
                        "NOTE: The order will NOT be finalized at this step. Order finalization (status update to FINISHED, warranty generation, "
                        +
                        "COD payment confirmation, and email notification) will only occur when the customer scans the QR code to confirm receipt.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Delivery confirmation created successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Order not found with orderId: {orderId}"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only DELIVERY role is allowed")
        })
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('DELIVERY')")
        public ApiResponse<DeliveryConfirmationResponse> createDeliveryConfirmation(
                        @Valid @RequestBody DeliveryConfirmationRequest request) {
                return ApiResponse.<DeliveryConfirmationResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Delivery confirmation created successfully")
                                .data(deliveryConfirmationService.createDeliveryConfirmation(request))
                                .build();
        }

        @GetMapping("/staff/{deliveryStaffId}")
        @Operation(summary = "Get delivery confirmations by delivery staff ID", description = "Retrieve all delivery confirmations for a specific delivery staff member. Only DELIVERY and ADMIN roles can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery confirmations retrieved successfully (may return empty list if no confirmations exist)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only DELIVERY and ADMIN roles are allowed")
        })
        @PreAuthorize("hasRole('DELIVERY') or hasRole('ADMIN')")
        public ApiResponse<List<DeliveryConfirmationResponse>> getDeliveryConfirmationsByStaff(
                        @PathVariable String deliveryStaffId) {
                return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Delivery confirmations retrieved successfully")
                                .data(deliveryConfirmationService.getDeliveryConfirmationsByStaff(deliveryStaffId))
                                .build();
        }

        // ========== CUSTOMER ENDPOINTS ==========

        @GetMapping("/order/{orderId}")
        @Operation(summary = "Get delivery confirmation by order ID", description = "Retrieve delivery confirmation information for a specific order. Only CUSTOMER (order owner) and ADMIN roles can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery confirmation retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery confirmation not found with orderId: {orderId}"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only CUSTOMER and ADMIN roles are allowed")
        })
        @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
        public ApiResponse<DeliveryConfirmationResponse> getDeliveryConfirmationByOrderId(@PathVariable Long orderId) {
                return ApiResponse.<DeliveryConfirmationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Delivery confirmation retrieved successfully")
                                .data(deliveryConfirmationService.getDeliveryConfirmationByOrderId(orderId))
                                .build();
        }

        @GetMapping("/customer/{customerId}")
        @Operation(summary = "Get delivery confirmations by customer ID", description = "Retrieve all delivery confirmations for a specific customer. Only CUSTOMER (themselves) and ADMIN roles can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery confirmations retrieved successfully (may return empty list if no confirmations exist)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only CUSTOMER and ADMIN roles are allowed")
        })
        @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
        public ApiResponse<List<DeliveryConfirmationResponse>> getDeliveryConfirmationsByCustomer(
                        @PathVariable String customerId) {
                return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Delivery confirmations retrieved successfully")
                                .data(deliveryConfirmationService.getDeliveryConfirmationsByCustomer(customerId))
                                .build();
        }

        @PostMapping("/scan-qr")
        @Operation(summary = "Scan QR code to confirm delivery receipt", description = "Scan QR code to confirm delivery receipt. Only CUSTOMER role can use this API. "
                        +
                        "Each QR code can only be scanned once. After successful scan, the system will: " +
                        "1) Update status to DELIVERED and set qrCodeScannedAt timestamp, " +
                        "2) Update order status to FINISHED, " +
                        "3) Confirm COD payment if applicable, " +
                        "4) Generate warranties for the order, " +
                        "5) Send email notification to customer. " +
                        "This is the final step that completes the entire order delivery process.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "QR code scanned successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "QR code already scanned - Each QR code can only be scanned once"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery confirmation not found with qrCode: {qrCode}"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only CUSTOMER role is allowed")
        })
        @PreAuthorize("hasRole('CUSTOMER')")
        public ApiResponse<DeliveryConfirmationResponse> scanQRCode(@Valid @RequestBody QRCodeScanRequest request) {
                return ApiResponse.<DeliveryConfirmationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("QR code scanned successfully")
                                .data(deliveryConfirmationService.scanQRCode(request))
                                .build();
        }

        @GetMapping("/qr/{qrCode}")
        @Operation(summary = "Get delivery confirmation by QR code", description = "Retrieve delivery confirmation information by QR code. Only CUSTOMER and ADMIN roles can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Delivery confirmation retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery confirmation not found with qrCode: {qrCode}"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only CUSTOMER and ADMIN roles are allowed")
        })
        @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
        public ApiResponse<DeliveryConfirmationResponse> getDeliveryConfirmationByQRCode(@PathVariable String qrCode) {
                return ApiResponse.<DeliveryConfirmationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Delivery confirmation retrieved successfully")
                                .data(deliveryConfirmationService.getDeliveryConfirmationByQRCode(qrCode))
                                .build();
        }

        // ========== ADMIN ENDPOINTS ==========

        @GetMapping
        @Operation(summary = "Get all delivery confirmations", description = "Retrieve all delivery confirmations in the system. Only ADMIN role can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "All delivery confirmations retrieved successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only ADMIN role is allowed")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<DeliveryConfirmationResponse>> getAllDeliveryConfirmations() {
                return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("All delivery confirmations retrieved successfully")
                                .data(deliveryConfirmationService.getAllDeliveryConfirmations())
                                .build();
        }

        @GetMapping("/scanned")
        @Operation(summary = "Get scanned delivery confirmations", description = "Retrieve all delivery confirmations that have been scanned (delivery receipt confirmed). Only ADMIN role can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Scanned delivery confirmations retrieved successfully (may return empty list)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only ADMIN role is allowed")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<DeliveryConfirmationResponse>> getScannedConfirmations() {
                return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Scanned delivery confirmations retrieved successfully")
                                .data(deliveryConfirmationService.getScannedConfirmations())
                                .build();
        }

        @GetMapping("/unscanned")
        @Operation(summary = "Get unscanned delivery confirmations", description = "Retrieve all delivery confirmations that have not been scanned (delivery receipt not confirmed). Only ADMIN role can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Unscanned delivery confirmations retrieved successfully (may return empty list)"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only ADMIN role is allowed")
        })
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<List<DeliveryConfirmationResponse>> getUnscannedConfirmations() {
                return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Unscanned delivery confirmations retrieved successfully")
                                .data(deliveryConfirmationService.getUnscannedConfirmations())
                                .build();
        }

        @PutMapping("/{confirmationId}/report-incident")
        @Operation(summary = "Report incident during delivery", description = "Delivery staff can report an incident during delivery (customer refused, product defect, etc.). " +
                        "This will set the delivery confirmation status to PENDING_REVIEW and update the order accordingly. Only DELIVERY role can use this API.")
        @ApiResponses(value = {
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Incident reported successfully"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request - Incident already reported or confirmation does not belong to this delivery staff"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Delivery confirmation not found"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthenticated"),
                        @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Access denied - Only DELIVERY role is allowed")
        })
        @PreAuthorize("hasRole('DELIVERY')")
        public ApiResponse<DeliveryConfirmationResponse> reportIncident(
                        @PathVariable Long confirmationId,
                        @Valid @RequestBody IncidentReportRequest request) {
                return ApiResponse.<DeliveryConfirmationResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Sự cố đã được ghi nhận. Đơn hàng chuyển sang trạng thái chờ xem xét.")
                                .data(deliveryConfirmationService.reportIncident(confirmationId, request))
                                .build();
        }
}
