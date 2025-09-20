package com.example.orderservice.controller;

import com.example.orderservice.enums.VoucherType;
import com.example.orderservice.request.VoucherRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.VoucherResponse;
import com.example.orderservice.service.inteface.VoucherService;
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
@RequestMapping("/api/vouchers")
@Tag(name = "Voucher Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class VoucherController {

    private final VoucherService voucherService;

    @PostMapping
    @Operation(summary = "Create new voucher")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<VoucherResponse> createVoucher(@Valid @RequestBody VoucherRequest request) {
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Voucher created successfully")
                .data(voucherService.createVoucher(request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get voucher by ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<VoucherResponse> getVoucherById(@PathVariable Integer id) {
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Voucher retrieved successfully")
                .data(voucherService.getVoucherById(id))
                .build();
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get voucher by code")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<VoucherResponse> getVoucherByCode(@PathVariable String code) {
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Voucher retrieved successfully")
                .data(voucherService.getVoucherByCode(code))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all vouchers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<List<VoucherResponse>> getAllVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Vouchers retrieved successfully")
                .data(voucherService.getAllVouchers())
                .build();
    }

    @GetMapping("/active")
    @Operation(summary = "Get active vouchers")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<List<VoucherResponse>> getActiveVouchers() {
        return ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Active vouchers retrieved successfully")
                .data(voucherService.getActiveVouchers())
                .build();
    }

    @GetMapping("/type/{type}")
    @Operation(summary = "Get vouchers by type")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<List<VoucherResponse>> getVouchersByType(@PathVariable VoucherType type) {
        return ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Vouchers retrieved successfully")
                .data(voucherService.getVouchersByType(type))
                .build();
    }

    @GetMapping("/order/{orderId}")
    @Operation(summary = "Get vouchers by order ID")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<List<VoucherResponse>> getVouchersByOrderId(@PathVariable Long orderId) {
        return ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Vouchers retrieved successfully")
                .data(voucherService.getVouchersByOrderId(orderId))
                .build();
    }

    @GetMapping("/applicable")
    @Operation(summary = "Get applicable vouchers for order amount")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<List<VoucherResponse>> getApplicableVouchers(@RequestParam Double orderAmount) {
        return ApiResponse.<List<VoucherResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Applicable vouchers retrieved successfully")
                .data(voucherService.getApplicableVouchers(orderAmount))
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update voucher")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<VoucherResponse> updateVoucher(@PathVariable Integer id, @Valid @RequestBody VoucherRequest request) {
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Voucher updated successfully")
                .data(voucherService.updateVoucher(id, request))
                .build();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete voucher")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deleteVoucher(@PathVariable Integer id) {
        voucherService.deleteVoucher(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.NO_CONTENT.value())
                .message("Voucher deleted successfully")
                .build();
    }

    // Voucher operation endpoints
    @PostMapping("/validate")
    @Operation(summary = "Validate voucher for order")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<VoucherResponse> validateVoucher(
            @RequestParam String code,
            @RequestParam Double orderAmount) {
        
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Voucher validated successfully")
                .data(voucherService.validateVoucher(code, orderAmount))
                .build();
    }

    @PostMapping("/apply")
    @Operation(summary = "Apply voucher to order")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<VoucherResponse> applyVoucher(
            @RequestParam String code,
            @RequestParam Long orderId) {
        
        return ApiResponse.<VoucherResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Voucher applied successfully")
                .data(voucherService.applyVoucher(code, orderId))
                .build();
    }

    @GetMapping("/calculate-discount")
    @Operation(summary = "Calculate discount for voucher")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
    public ApiResponse<Double> calculateDiscount(
            @RequestParam String voucherCode,
            @RequestParam Double orderAmount) {
        
        return ApiResponse.<Double>builder()
                .status(HttpStatus.OK.value())
                .message("Discount calculated successfully")
                .data(voucherService.calculateDiscount(voucherCode, orderAmount))
                .build();
    }

    @PostMapping("/{id}/increment-usage")
    @Operation(summary = "Increment voucher usage count")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<Void> incrementUsageCount(@PathVariable Integer id) {
        voucherService.incrementUsageCount(id);
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Usage count incremented successfully")
                .build();
    }

    @PostMapping("/expire-vouchers")
    @Operation(summary = "Expire outdated vouchers")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> expireVouchers() {
        voucherService.expireVouchers();
        return ApiResponse.<Void>builder()
                .status(HttpStatus.OK.value())
                .message("Expired vouchers processed successfully")
                .build();
    }
}
