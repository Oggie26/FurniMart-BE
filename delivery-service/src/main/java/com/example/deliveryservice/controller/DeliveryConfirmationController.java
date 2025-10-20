package com.example.deliveryservice.controller;

import com.example.deliveryservice.request.DeliveryConfirmationRequest;
import com.example.deliveryservice.request.QRCodeScanRequest;
import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.DeliveryConfirmationResponse;
import com.example.deliveryservice.service.inteface.DeliveryConfirmationService;
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
@RequestMapping("/api/delivery-confirmations")
@Tag(name = "Delivery Confirmation Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class DeliveryConfirmationController {

    private final DeliveryConfirmationService deliveryConfirmationService;

    // ========== DELIVERY STAFF ENDPOINTS ==========

    @PostMapping
    @Operation(summary = "Create delivery confirmation with photos (Delivery Staff only)")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('DELIVERY_STAFF') or hasRole('DELIVERER')")
    public ApiResponse<DeliveryConfirmationResponse> createDeliveryConfirmation(@Valid @RequestBody DeliveryConfirmationRequest request) {
        return ApiResponse.<DeliveryConfirmationResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Delivery confirmation created successfully")
                .data(deliveryConfirmationService.createDeliveryConfirmation(request))
                .build();
    }

    @GetMapping("/staff/{deliveryStaffId}")
    @Operation(summary = "Get delivery confirmations by delivery staff ID")
    @PreAuthorize("hasRole('DELIVERY_STAFF') or hasRole('DELIVERER') or hasRole('ADMIN')")
    public ApiResponse<List<DeliveryConfirmationResponse>> getDeliveryConfirmationsByStaff(@PathVariable String deliveryStaffId) {
        return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery confirmations retrieved successfully")
                .data(deliveryConfirmationService.getDeliveryConfirmationsByStaff(deliveryStaffId))
                .build();
    }

    // ========== CUSTOMER ENDPOINTS ==========

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get delivery confirmation by order ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<DeliveryConfirmationResponse> getDeliveryConfirmationByOrderId(@PathVariable Long orderId) {
        return ApiResponse.<DeliveryConfirmationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery confirmation retrieved successfully")
                .data(deliveryConfirmationService.getDeliveryConfirmationByOrderId(orderId))
                .build();
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get delivery confirmations by customer ID")
    @PreAuthorize("hasRole('CUSTOMER') or hasRole('ADMIN')")
    public ApiResponse<List<DeliveryConfirmationResponse>> getDeliveryConfirmationsByCustomer(@PathVariable String customerId) {
        return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Delivery confirmations retrieved successfully")
                .data(deliveryConfirmationService.getDeliveryConfirmationsByCustomer(customerId))
                .build();
    }

    @PostMapping("/scan-qr")
    @Operation(summary = "Scan QR code to confirm delivery receipt")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<DeliveryConfirmationResponse> scanQRCode(@Valid @RequestBody QRCodeScanRequest request) {
        return ApiResponse.<DeliveryConfirmationResponse>builder()
                .status(HttpStatus.OK.value())
                .message("QR code scanned successfully")
                .data(deliveryConfirmationService.scanQRCode(request))
                .build();
    }

    @GetMapping("/qr/{qrCode}")
    @Operation(summary = "Get delivery confirmation by QR code")
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
    @Operation(summary = "Get all delivery confirmations (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DeliveryConfirmationResponse>> getAllDeliveryConfirmations() {
        return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("All delivery confirmations retrieved successfully")
                .data(deliveryConfirmationService.getAllDeliveryConfirmations())
                .build();
    }

    @GetMapping("/scanned")
    @Operation(summary = "Get scanned delivery confirmations (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DeliveryConfirmationResponse>> getScannedConfirmations() {
        return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Scanned delivery confirmations retrieved successfully")
                .data(deliveryConfirmationService.getScannedConfirmations())
                .build();
    }

    @GetMapping("/unscanned")
    @Operation(summary = "Get unscanned delivery confirmations (Admin only)")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<List<DeliveryConfirmationResponse>> getUnscannedConfirmations() {
        return ApiResponse.<List<DeliveryConfirmationResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Unscanned delivery confirmations retrieved successfully")
                .data(deliveryConfirmationService.getUnscannedConfirmations())
                .build();
    }
}


