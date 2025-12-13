package com.example.userservice.controller;

import com.example.userservice.request.WalletRequest;
import com.example.userservice.request.WalletTransactionRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WalletResponse;
import com.example.userservice.response.WalletTransactionResponse;
import com.example.userservice.service.inteface.WalletService;
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
@RequestMapping("/api/wallets")
@Tag(name = "Wallet Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping
    @Operation(summary = "Create new wallet - Only ADMIN can create wallets")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Wallet created successfully")
                .data(walletService.createWallet(request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by ID - ADMIN and STAFF can view")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<WalletResponse> getWalletById(@PathVariable String id) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(walletService.getWalletById(id))
                .build();
    }

    @GetMapping("/my-wallet")
    @Operation(summary = "Get current user's wallet - Only CUSTOMER can view their own wallet")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<WalletResponse> getMyWallet() {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(walletService.getMyWallet())
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID - ADMIN and STAFF can view user wallets")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
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
    @Operation(summary = "Update wallet - Only ADMIN can modify wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> updateWallet(@PathVariable String id, @Valid @RequestBody WalletRequest request) {
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

    // Transaction endpoints
    @PostMapping("/transactions")
    @Operation(summary = "Create wallet transaction - Only ADMIN can create transactions")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletTransactionResponse> createTransaction(@Valid @RequestBody WalletTransactionRequest request) {
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
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<List<WalletTransactionResponse>> getTransactionsByWalletId(@PathVariable String walletId) {
        return ApiResponse.<List<WalletTransactionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Transactions retrieved successfully")
                .data(walletService.getTransactionsByWalletId(walletId))
                .build();
    }

    @GetMapping("/{walletId}/transactions/paged")
    @Operation(summary = "Get transactions by wallet ID with pagination")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getTransactionsByWalletIdPaged(
            @PathVariable String walletId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService.getTransactionsByWalletId(walletId, pageable);
        
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

    // Wallet operation endpoints
    @PostMapping("/{walletId}/deposit")
    @Operation(summary = "Deposit to wallet - Only ADMIN can modify wallets")
    @PreAuthorize("hasRole('ADMIN')")
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
    @Operation(summary = "Withdraw from wallet - Only ADMIN can modify wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> withdraw(
            @PathVariable String walletId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String referenceId) {
        
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal completed successfully")
                .data(walletService.withdraw(walletId, amount, description, referenceId))
                .build();
    }

    @PostMapping("/transfer")
    @Operation(summary = "Transfer between wallets - Only ADMIN can modify wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> transfer(
            @RequestParam String fromWalletId,
            @RequestParam String toWalletId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String referenceId) {
        
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Transfer completed successfully")
                .data(walletService.transfer(fromWalletId, toWalletId, amount, description, referenceId))
                .build();
    }

    @GetMapping("/{walletId}/balance")
    @Operation(summary = "Get wallet balance")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<Double> getWalletBalance(@PathVariable String walletId) {
        return ApiResponse.<Double>builder()
                .status(HttpStatus.OK.value())
                .message("Balance retrieved successfully")
                .data(walletService.getWalletBalance(walletId))
                .build();
    }

    @PostMapping("/{walletId}/refund-to-vnpay")
    @Operation(summary = "Refund from wallet to VNPay - Only ADMIN can modify wallets")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<WalletResponse> refundToVNPay(
            @PathVariable String walletId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String orderId) {
        
        String refundDescription = description != null ? description : 
            "Refund to VNPay" + (orderId != null ? " for order #" + orderId : "");
        
        // Withdraw from wallet (this creates the transaction)
        WalletResponse walletResponse = walletService.withdraw(
            walletId, 
            amount, 
            refundDescription, 
            orderId != null ? "ORDER_" + orderId : null
        );
        
        // Note: In a production system, you would also call VNPay's refund API here
        // to actually process the refund through VNPay's payment gateway
        // This would typically involve:
        // 1. Calling VNPay's refund endpoint with transaction reference
        // 2. Handling the response and updating payment status
        
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Refund to VNPay processed successfully. Amount: " + amount + " VNĐ")
                .data(walletResponse)
                .build();
    }

    // ========== CUSTOMER ENDPOINTS ==========
    // These endpoints allow CUSTOMERs to manage their own wallets

    @PostMapping("/my-wallet/deposit")
    @Operation(summary = "Deposit to current user's wallet via VNPay - CUSTOMER only")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<String> depositToMyWallet(
            @RequestParam Double amount,
            @RequestParam(required = false, defaultValue = "127.0.0.1") String ipAddress) {
        
        // Get current user's wallet to extract walletId
        // getMyWallet() uses SecurityContextHolder internally to get current user
        WalletResponse myWallet = walletService.getMyWallet();
        String walletId = myWallet.getId();
        
        // Call depositViaVNPay service method which returns payment URL
        // This creates a PENDING transaction and returns VNPay payment URL
        String paymentUrl = walletService.depositViaVNPay(walletId, amount, ipAddress);
        
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Payment URL generated successfully. Please complete payment via VNPay.")
                .data(paymentUrl)
                .build();
    }

    @PostMapping("/my-wallet/withdraw")
    @Operation(summary = "Withdraw from current user's wallet - CUSTOMER only")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<WalletResponse> withdrawFromMyWallet(
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String referenceId) {
        
        // Get current user's wallet
        WalletResponse myWallet = walletService.getMyWallet();
        String walletId = myWallet.getId();
        
        // Call withdraw service method
        WalletResponse walletResponse = walletService.withdraw(walletId, amount, description, referenceId);
        
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal completed successfully")
                .data(walletResponse)
                .build();
    }

    @GetMapping("/my-wallet/transactions")
    @Operation(summary = "Get transaction history for current user's wallet - CUSTOMER only")
    @PreAuthorize("hasRole('CUSTOMER')")
    public ApiResponse<List<WalletTransactionResponse>> getMyWalletTransactions() {
        
        // Get current user's wallet
        WalletResponse myWallet = walletService.getMyWallet();
        String walletId = myWallet.getId();
        
        // Get transaction history for this wallet
        List<WalletTransactionResponse> transactions = walletService.getTransactionsByWalletId(walletId);
        
        return ApiResponse.<List<WalletTransactionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Transaction history retrieved successfully")
                .data(transactions)
                .build();
    }
}
