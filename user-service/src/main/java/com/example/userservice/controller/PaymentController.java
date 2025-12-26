package com.example.userservice.controller;

import com.example.userservice.request.WalletTransactionRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.PageResponse;
import com.example.userservice.response.WalletResponse;
import com.example.userservice.response.WalletTransactionResponse;
import com.example.userservice.service.inteface.WalletService;
import com.example.userservice.enums.WalletTransactionType;
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

import java.math.BigDecimal;
import java.util.UUID;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment & Escrow Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

    private final WalletService walletService;

    @GetMapping
    @Operation(summary = "Get all payment transactions - Admin only")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getAllPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService.getPaymentTransactions(pageable);

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
                .message("Payment transactions retrieved successfully")
                .data(pageResponse)
                .build();
    }

    @GetMapping("/escrow")
    @Operation(summary = "Get all escrow transactions - Admin only")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<PageResponse<WalletTransactionResponse>> getAllEscrowTransactions(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        Pageable pageable = PageRequest.of(page, size);
        Page<WalletTransactionResponse> transactions = walletService.getEscrowTransactions(pageable);

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
                .message("Escrow transactions retrieved successfully")
                .data(pageResponse)
                .build();
    }

    @PostMapping("/escrow/deposit/{walletId}")
    @Operation(summary = "Deposit to escrow (Hold funds) - Admin only")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<WalletTransactionResponse> depositToEscrow(
            @PathVariable String walletId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String referenceId) {

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .code("ESC-DEP-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.ESCROW_DEPOSIT)
                .description(description != null ? description : "Escrow Deposit")
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        return ApiResponse.<WalletTransactionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Escrow deposit successful")
                .data(walletService.createTransaction(request))
                .build();
    }

    @PostMapping("/escrow/withdraw/{walletId}")
    @Operation(summary = "Withdraw from escrow (Release funds/Refund) - Admin only")
    @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
    public ApiResponse<WalletTransactionResponse> withdrawFromEscrow(
            @PathVariable String walletId,
            @RequestParam Double amount,
            @RequestParam(required = false) String description,
            @RequestParam(required = false) String referenceId) {

        WalletTransactionRequest request = WalletTransactionRequest.builder()
                .code("ESC-WDR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase())
                .amount(BigDecimal.valueOf(amount))
                .type(WalletTransactionType.ESCROW_WITHDRAWAL)
                .description(description != null ? description : "Escrow Withdrawal")
                .referenceId(referenceId)
                .walletId(walletId)
                .build();

        return ApiResponse.<WalletTransactionResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Escrow withdrawal successful")
                .data(walletService.createTransaction(request))
                .build();
    }
}
