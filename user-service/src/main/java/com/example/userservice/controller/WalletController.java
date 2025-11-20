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
    @Operation(
            summary = "Create new wallet",
            description = "Create a new wallet manually. Note: Wallets are automatically created for CUSTOMER users during registration. " +
                    "This API is primarily for ADMIN/STAFF to create wallets manually if needed. " +
                    "Each user can only have one wallet."
    )
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<WalletResponse> createWallet(@Valid @RequestBody WalletRequest request) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Wallet created successfully")
                .data(walletService.createWallet(request))
                .build();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get wallet by ID")
    public ApiResponse<WalletResponse> getWalletById(@PathVariable String id) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(walletService.getWalletById(id))
                .build();
    }

    @GetMapping("/user/{userId}")
    @Operation(summary = "Get wallet by user ID")
    public ApiResponse<WalletResponse> getWalletByUserId(@PathVariable String userId) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(walletService.getWalletByUserId(userId))
                .build();
    }

    @GetMapping("/code/{code}")
    @Operation(summary = "Get wallet by code")
    public ApiResponse<WalletResponse> getWalletByCode(@PathVariable String code) {
        return ApiResponse.<WalletResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Wallet retrieved successfully")
                .data(walletService.getWalletByCode(code))
                .build();
    }

    @GetMapping
    @Operation(summary = "Get all wallets")
    public ApiResponse<List<WalletResponse>> getAllWallets() {
        return ApiResponse.<List<WalletResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Wallets retrieved successfully")
                .data(walletService.getAllWallets())
                .build();
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update wallet")
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
    public ApiResponse<WalletTransactionResponse> createTransaction(@Valid @RequestBody WalletTransactionRequest request) {
        return ApiResponse.<WalletTransactionResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Transaction created successfully")
                .data(walletService.createTransaction(request))
                .build();
    }

    @GetMapping("/transactions/{id}")
    @Operation(summary = "Get transaction by ID")
    public ApiResponse<WalletTransactionResponse> getTransactionById(@PathVariable String id) {
        return ApiResponse.<WalletTransactionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Transaction retrieved successfully")
                .data(walletService.getTransactionById(id))
                .build();
    }

    @GetMapping("/{walletId}/transactions")
    @Operation(summary = "Get transactions by wallet ID")
    public ApiResponse<List<WalletTransactionResponse>> getTransactionsByWalletId(@PathVariable String walletId) {
        return ApiResponse.<List<WalletTransactionResponse>>builder()
                .status(HttpStatus.OK.value())
                .message("Transactions retrieved successfully")
                .data(walletService.getTransactionsByWalletId(walletId))
                .build();
    }

    @GetMapping("/{walletId}/transactions/paged")
    @Operation(summary = "Get transactions by wallet ID with pagination")
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

    @PostMapping("/{walletId}/deposit")
    @Operation(summary = "Deposit to wallet")
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
    @Operation(summary = "Transfer between wallets")
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
    public ApiResponse<Double> getWalletBalance(@PathVariable String walletId) {
        return ApiResponse.<Double>builder()
                .status(HttpStatus.OK.value())
                .message("Balance retrieved successfully")
                .data(walletService.getWalletBalance(walletId))
                .build();
    }

    @PostMapping("/withdraw-to-vnpay")
    @Operation(
            summary = "Withdraw money from wallet to VNPay bank account",
            description = "Withdraw money from wallet to a bank account via VNPay. " +
                    "The amount will be deducted from wallet and transferred to the specified bank account. " +
                    "Minimum withdrawal: 10,000 VND, Maximum: 100,000,000 VND. " +
                    "Transaction will be created with PENDING status first, then updated to COMPLETED or FAILED based on VNPay processing result."
    )
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<WalletTransactionResponse> withdrawToVNPay(
            @Valid @RequestBody com.example.userservice.request.WalletWithdrawToVNPayRequest request) {
        return ApiResponse.<WalletTransactionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Withdrawal request processed successfully")
                .data(walletService.withdrawToVNPay(
                        request.getWalletId(),
                        request.getAmount(),
                        request.getBankAccountNumber(),
                        request.getBankName(),
                        request.getAccountHolderName(),
                        request.getDescription()
                ))
                .build();
    }

    @PostMapping("/{walletId}/deposit-via-vnpay")
    @Operation(
            summary = "Deposit money to wallet via VNPay payment gateway",
            description = "Create a deposit request and return VNPay payment URL. " +
                    "User will be redirected to VNPay to complete the payment. " +
                    "After successful payment, the amount will be deposited to the wallet. " +
                    "Transaction will be created with PENDING status first, then updated to COMPLETED or FAILED based on VNPay payment result."
    )
    @ResponseStatus(HttpStatus.OK)
    public ApiResponse<String> depositViaVNPay(
            @PathVariable String walletId,
            @RequestParam Double amount,
            @RequestHeader(value = "X-Forwarded-For", required = false) String forwardedFor,
            @RequestHeader(value = "X-Real-IP", required = false) String realIp) {
        
        // Get client IP address
        String clientIp = realIp != null ? realIp : 
                         (forwardedFor != null ? forwardedFor.split(",")[0].trim() : "127.0.0.1");
        
        String paymentUrl = walletService.depositViaVNPay(walletId, amount, clientIp);
        
        return ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("VNPay payment URL created successfully")
                .data(paymentUrl)
                .build();
    }
}
