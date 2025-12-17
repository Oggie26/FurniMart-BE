package com.example.userservice.controller;

import com.example.userservice.request.WalletWithdrawToVNPayRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WalletWithdrawalRequestResponse;
import com.example.userservice.service.inteface.WalletWithdrawalRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/wallets/withdrawal-requests")
@Tag(name = "Wallet Withdrawal Request Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletWithdrawalRequestController {

    private final WalletWithdrawalRequestService withdrawalRequestService;

    /**
     * User: Tạo yêu cầu rút tiền về VNPay (tự động xử lý, không cần admin duyệt)
     */
    @PostMapping
    @Operation(summary = "Create withdrawal request to VNPay (automatically processed, no admin approval needed)")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('CUSTOMER', 'USER')")
    public ApiResponse<WalletWithdrawalRequestResponse> createWithdrawalRequest(
            @Valid @RequestBody WalletWithdrawToVNPayRequest request) {
        return ApiResponse.<WalletWithdrawalRequestResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Withdrawal request created and processing. Money will be transferred to your bank account.")
                .data(withdrawalRequestService.createWithdrawalRequest(request))
                .build();
    }

    /**
     * User: Xem yêu cầu rút tiền của mình theo ID
     */
    @GetMapping("/{requestId}")
    @Operation(summary = "Get my withdrawal request by ID")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'USER')")
    public ApiResponse<WalletWithdrawalRequestResponse> getMyWithdrawalRequest(
            @PathVariable String requestId) {
        return ApiResponse.<WalletWithdrawalRequestResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal request retrieved successfully")
                .data(withdrawalRequestService.getMyWithdrawalRequest(requestId))
                .build();
    }

    /**
     * User: Xem tất cả yêu cầu rút tiền của mình
     */
    @GetMapping("/my-requests")
    @Operation(summary = "Get all my withdrawal requests")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'USER')")
    public ApiResponse<List<WalletWithdrawalRequestResponse>> getMyWithdrawalRequests() {
        return ApiResponse.<List<WalletWithdrawalRequestResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal requests retrieved successfully")
                .data(withdrawalRequestService.getMyWithdrawalRequests())
                .build();
    }

    /**
     * User: Hủy yêu cầu rút tiền (chỉ khi status = PENDING_APPROVAL hoặc PROCESSING)
     */
    @DeleteMapping("/{requestId}")
    @Operation(summary = "Cancel my withdrawal request (only if status is PENDING_APPROVAL or PROCESSING)")
    @PreAuthorize("hasAnyRole('CUSTOMER', 'USER')")
    public ApiResponse<WalletWithdrawalRequestResponse> cancelMyWithdrawalRequest(
            @PathVariable String requestId) {
        return ApiResponse.<WalletWithdrawalRequestResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal request cancelled successfully")
                .data(withdrawalRequestService.cancelMyWithdrawalRequest(requestId))
                .build();
    }

    /**
     * Admin: Xem tất cả yêu cầu rút tiền (có phân trang)
     */
    @GetMapping
    @Operation(summary = "Get all withdrawal requests (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<PageResponse<WalletWithdrawalRequestResponse>> getAllWithdrawalRequests(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletWithdrawalRequestResponse> requests = withdrawalRequestService.getAllWithdrawalRequests(pageable);
        
        PageResponse<WalletWithdrawalRequestResponse> pageResponse = PageResponse.<WalletWithdrawalRequestResponse>builder()
                .content(requests.getContent())
                .number(requests.getNumber())
                .size(requests.getSize())
                .totalElements(requests.getTotalElements())
                .totalPages(requests.getTotalPages())
                .first(requests.isFirst())
                .last(requests.isLast())
                .build();

        return ApiResponse.<PageResponse<WalletWithdrawalRequestResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal requests retrieved successfully")
                .data(pageResponse)
                .build();
    }

    /**
     * Admin: Xem yêu cầu rút tiền theo status
     */
    @GetMapping("/status/{status}")
    @Operation(summary = "Get withdrawal requests by status (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<List<WalletWithdrawalRequestResponse>> getWithdrawalRequestsByStatus(
            @PathVariable String status) {
        return ApiResponse.<List<WalletWithdrawalRequestResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal requests retrieved successfully")
                .data(withdrawalRequestService.getWithdrawalRequestsByStatus(status))
                .build();
    }

    /**
     * Admin: Xem chi tiết yêu cầu rút tiền
     */
    @GetMapping("/admin/{requestId}")
    @Operation(summary = "Get withdrawal request by ID (Admin only)")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<WalletWithdrawalRequestResponse> getWithdrawalRequestById(
            @PathVariable String requestId) {
        return ApiResponse.<WalletWithdrawalRequestResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal request retrieved successfully")
                .data(withdrawalRequestService.getWithdrawalRequestById(requestId))
                .build();
    }

    // Note: Approve/Reject endpoints removed - withdrawals are now automatically processed
    // Admin can still view all withdrawal requests for monitoring purposes
}
