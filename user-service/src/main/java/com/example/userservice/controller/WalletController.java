package com.example.userservice.controller;

import com.example.userservice.entity.Wallet;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.request.WalletRequest;
import com.example.userservice.request.WalletTransactionRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WalletResponse;
import com.example.userservice.response.WalletTransactionResponse;
import com.example.userservice.service.VNPayWithdrawalService;
import com.example.userservice.service.inteface.WalletService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
@RequestMapping("/api/wallets")
@Tag(name = "Wallet Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class WalletController {

        private final WalletService walletService;
        private final VNPayWithdrawalService vnPayWithdrawalService;
        private final WalletRepository walletRepository;

        @PostMapping
        @Operation(summary = "Create new wallet")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Wallet created successfully")
                                .data(walletService.createWallet(request))
                                .build();
        }

        @GetMapping("/{id}")
        @Operation(summary = "Get wallet by ID")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<WalletResponse> getWalletById(@PathVariable String id) {
                walletService.validateWalletAccess(id);
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallet retrieved successfully")
                                .data(walletService.getWalletById(id))
                                .build();
        }

        @GetMapping("/my-wallet")
        @Operation(summary = "Get current user's wallet - Only for customers to view their own wallet")
        @PreAuthorize("hasAnyRole('CUSTOMER', 'USER')")
        public ApiResponse<WalletResponse> getMyWallet() {
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallet retrieved successfully")
                                .data(walletService.getMyWallet())
                                .build();
        }

        @GetMapping("/user/{userId}")
        @Operation(summary = "Get wallet by user ID - For admin and staff to manage user wallets")
        public ApiResponse<WalletResponse> getWalletByUserId(@PathVariable String userId) {
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallet retrieved successfully")
                                .data(walletService.getWalletByUserId(userId))
                                .build();
        }

        @GetMapping("/code/{code}")
        @Operation(summary = "Get wallet by code")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletResponse> getWalletByCode(@PathVariable String code) {
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallet retrieved successfully")
                                .data(walletService.getWalletByCode(code))
                                .build();
        }

        @GetMapping
        @Operation(summary = "Get all wallets")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<List<WalletResponse>> getAllWallets() {
                return ApiResponse.<List<WalletResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallets retrieved successfully")
                                .data(walletService.getAllWallets())
                                .build();
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update wallet")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletResponse> updateWallet(@PathVariable String id,
                        @Valid @RequestBody WalletRequest request) {
                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Wallet updated successfully")
                                .data(walletService.updateWallet(id, request))
                                .build();
        }

        @DeleteMapping("/{id}")
        @Operation(summary = "Delete wallet")
        @ResponseStatus(HttpStatus.NO_CONTENT)
        @PreAuthorize("hasRole('ADMIN')")
        public ApiResponse<Void> deleteWallet(@PathVariable String id) {
                walletService.deleteWallet(id);
                return ApiResponse.<Void>builder()
                                .status(HttpStatus.NO_CONTENT.value())
                                .message("Wallet deleted successfully")
                                .build();
        }

        @PostMapping("/transactions")
        @Operation(summary = "Create wallet transaction")
        @ResponseStatus(HttpStatus.CREATED)
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletTransactionResponse> createTransaction(
                        @Valid @RequestBody WalletTransactionRequest request) {
                return ApiResponse.<WalletTransactionResponse>builder()
                                .status(HttpStatus.CREATED.value())
                                .message("Transaction created successfully")
                                .data(walletService.createTransaction(request))
                                .build();
        }

        @GetMapping("/transactions/{id}")
        @Operation(summary = "Get transaction by ID")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletTransactionResponse> getTransactionById(@PathVariable String id) {
                return ApiResponse.<WalletTransactionResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Transaction retrieved successfully")
                                .data(walletService.getTransactionById(id))
                                .build();
        }

        @GetMapping("/{walletId}/transactions")
        @Operation(summary = "Get transactions by wallet ID")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<List<WalletTransactionResponse>> getTransactionsByWalletId(@PathVariable String walletId) {
                walletService.validateWalletAccess(walletId);
                return ApiResponse.<List<WalletTransactionResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Transactions retrieved successfully")
                                .data(walletService.getTransactionsByWalletId(walletId))
                                .build();
        }

        @GetMapping("/{walletId}/transactions/paged")
        @Operation(summary = "Get transactions by wallet ID with pagination")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<PageResponse<WalletTransactionResponse>> getTransactionsByWalletIdPaged(
                        @PathVariable String walletId,
                        @RequestParam(defaultValue = "0") int page,
                        @RequestParam(defaultValue = "10") int size) {
                walletService.validateWalletAccess(walletId);

                Pageable pageable = PageRequest.of(page, size);
                Page<WalletTransactionResponse> transactions = walletService.getTransactionsByWalletId(walletId,
                                pageable);

                PageResponse<WalletTransactionResponse> pageResponse = PageResponse.<WalletTransactionResponse>builder()
                                .content(transactions.getContent())
                                .number(transactions.getNumber())
                                .size(transactions.getSize())
                                .totalElements(transactions.getTotalElements())
                                .totalPages(transactions.getTotalPages())
                                .first(transactions.isFirst())
                                .last(transactions.isLast())
                                .build();

                return ApiResponse.<PageResponse<WalletTransactionResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Transactions retrieved successfully")
                                .data(pageResponse)
                                .build();
        }

        @PostMapping("/{walletId}/deposit")
        @Operation(summary = "Deposit to wallet")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<WalletResponse> deposit(
                        @PathVariable String walletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String referenceId) {

                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Deposit completed successfully")
                                .data(walletService.deposit(walletId, amount, description, referenceId))
                                .build();
        }

        @PostMapping("/{walletId}/withdraw")
        @Operation(summary = "Withdraw from wallet")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<WalletResponse> withdraw(
                        @PathVariable String walletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String referenceId) {
                walletService.validateWalletAccess(walletId);

                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Withdrawal completed successfully")
                                .data(walletService.withdraw(walletId, amount, description, referenceId))
                                .build();
        }

        @PostMapping("/{walletId}/refund")
        @Operation(summary = "Refund to wallet")
        public ApiResponse<WalletResponse> refund(
                        @PathVariable String walletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String referenceId,
                        HttpServletRequest request) {

                String internalKey = request.getHeader("X-Internal-Sys");
                boolean isInternal = "FURNIMART_INTERNAL_KEY".equals(internalKey);

                if (!isInternal) {
                        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder
                                        .getContext().getAuthentication();

                        boolean hasPermission = auth.getAuthorities().stream()
                                        .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")
                                                        || a.getAuthority().equals("ROLE_STAFF"));

                        if (!hasPermission) {
                                log.warn("Access denied to refund endpoint for user: {}", auth.getName());
                                throw new RuntimeException(
                                                "Access Denied: Requires ADMIN/STAFF role or Internal System Key");
                        }
                }

                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Refund completed successfully")
                                .data(walletService.refund(walletId, amount, description, referenceId))
                                .build();
        }

        @PostMapping("/transfer")
        @Operation(summary = "Transfer between wallets")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<WalletResponse> transfer(
                        @RequestParam String fromWalletId,
                        @RequestParam String toWalletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) String referenceId) {
                walletService.validateWalletAccess(fromWalletId);

                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message("Transfer completed successfully")
                                .data(walletService.transfer(fromWalletId, toWalletId, amount, description,
                                                referenceId))
                                .build();
        }

        @GetMapping()
        @Operation(summary = "Get Transaction Wallet")
        public ApiResponse<List<Wallet>> getTransactionWallet(){
                return ApiResponse.<List<Wallet>>builder()
                        .status(HttpStatus.OK.value())
                        .message("Transfer completed successfully")
                        .data(walletRepository.findAll())
                        .build();
        }

        @GetMapping("/{walletId}/balance")
        @Operation(summary = "Get wallet balance")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF') or hasRole('CUSTOMER')")
        public ApiResponse<Double> getWalletBalance(@PathVariable String walletId) {
                walletService.validateWalletAccess(walletId);
                return ApiResponse.<Double>builder()
                                .status(HttpStatus.OK.value())
                                .message("Balance retrieved successfully")
                                .data(walletService.getWalletBalance(walletId))
                                .build();
        }

        @PostMapping("/{walletId}/refund-to-vnpay")
        @Operation(summary = "Refund from wallet to VNPay (integrated with VNPay sandbox refund API)")
        public ApiResponse<WalletResponse> refundToVNPay(
                        @PathVariable String walletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) Long orderId,
                        @RequestParam(required = false) String vnpTransactionNo,
                        @RequestParam(required = false) String originalTransactionDate,
                        @RequestParam(required = false, defaultValue = "true") Boolean isFullRefund,
                        HttpServletRequest request) {

                String refundDescription = description != null ? description
                                : "Refund to VNPay" + (orderId != null ? " for order #" + orderId : "");

                WalletResponse walletResponse = walletService.withdraw(
                                walletId,
                                amount,
                                refundDescription,
                                orderId != null ? "ORDER_" + orderId : null);

                boolean vnpayRefundSuccess = false;
                String originalTxnRef = orderId != null ? orderId.toString() : walletId;

                if (vnpTransactionNo != null && !vnpTransactionNo.isEmpty()
                                && originalTransactionDate != null && !originalTransactionDate.isEmpty()) {
                        try {
                                String ipAddress = getClientIpAddress(request);

                                vnpayRefundSuccess = vnPayWithdrawalService.processRefund(
                                                originalTxnRef,
                                                vnpTransactionNo,
                                                originalTransactionDate,
                                                amount,
                                                isFullRefund != null && isFullRefund,
                                                refundDescription,
                                                ipAddress);
                        } catch (Exception e) {
                                log.error("Error calling VNPay refund API: {}", e.getMessage());
                        }
                }

                String message = vnpayRefundSuccess
                                ? "Refund to VNPay processed successfully via VNPay API. Amount: " + amount + " VNĐ"
                                : (vnpTransactionNo != null
                                                ? "Wallet withdrawal completed. VNPay refund API call failed or pending."
                                                : "Refund to VNPay processed successfully (wallet withdrawal only). Amount: "
                                                                + amount + " VNĐ");

                return ApiResponse.<WalletResponse>builder()
                                .status(HttpStatus.OK.value())
                                .message(message)
                                .data(walletResponse)
                                .build();
        }


        private String getClientIpAddress(HttpServletRequest request) {
                String ipAddress = request.getHeader("X-Forwarded-For");
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                        ipAddress = request.getHeader("X-Real-IP");
                }
                if (ipAddress == null || ipAddress.isEmpty() || "unknown".equalsIgnoreCase(ipAddress)) {
                        ipAddress = request.getRemoteAddr();
                }
                if (ipAddress != null && ipAddress.contains(",")) {
                        ipAddress = ipAddress.split(",")[0].trim();
                }
                return ipAddress != null ? ipAddress : "127.0.0.1";
        }
}
