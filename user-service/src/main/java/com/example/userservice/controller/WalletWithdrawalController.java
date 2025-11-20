package com.example.userservice.controller;

import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.repository.WalletTransactionRepository;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.service.VNPayWithdrawalService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller để xử lý webhook callback từ VNPay cho withdrawal
 */
@RestController
@RequestMapping("/api/wallets/withdraw-to-vnpay")
@RequiredArgsConstructor
@Slf4j
public class WalletWithdrawalController {

    private final VNPayWithdrawalService vnPayWithdrawalService;
    private final WalletTransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    /**
     * Webhook callback từ VNPay để cập nhật trạng thái withdrawal
     * VNPay sẽ gọi endpoint này sau khi xử lý withdrawal request
     */
    @PostMapping("/callback")
    public ApiResponse<String> handleWithdrawalCallback(@RequestParam Map<String, String> params) {
        log.info("Received VNPay withdrawal callback: {}", params);

        try {
            // Validate signature
            if (!vnPayWithdrawalService.validateWithdrawalCallback(params)) {
                log.error("Invalid VNPay withdrawal callback signature");
                return ApiResponse.<String>builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message("Invalid signature")
                        .build();
            }

            // Extract transaction info
            String referenceId = params.get("vnp_TxnRef");
            String responseCode = params.get("vnp_ResponseCode");
            String transactionNo = params.get("vnp_TransactionNo");
            String amount = params.get("vnp_Amount");

            log.info("Processing withdrawal callback: Reference={}, ResponseCode={}, TransactionNo={}", 
                    referenceId, responseCode, transactionNo);

            // Find transaction by referenceId
            WalletTransaction transaction = transactionRepository.findByReferenceIdAndIsDeletedFalse(referenceId)
                    .orElse(null);

            if (transaction == null) {
                log.error("Transaction not found for reference: {}", referenceId);
                return ApiResponse.<String>builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message("Transaction not found")
                        .build();
            }

            // Update transaction status based on VNPay response
            if ("00".equals(responseCode)) {
                // Success
                transaction.setStatus(WalletTransactionStatus.COMPLETED);
                transactionRepository.save(transaction);
                log.info("Withdrawal completed successfully. Transaction: {}, Reference: {}", 
                        transaction.getCode(), referenceId);
                
                return ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Withdrawal processed successfully")
                        .data("SUCCESS")
                        .build();
            } else {
                // Failed - rollback wallet balance
                var wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId())
                        .orElse(null);
                
                if (wallet != null) {
                    // Refund amount back to wallet
                    wallet.setBalance(wallet.getBalance().add(transaction.getAmount()));
                    walletRepository.save(wallet);
                    log.info("Refunded {} VND to wallet {} due to withdrawal failure", 
                            transaction.getAmount(), wallet.getId());
                }

                transaction.setStatus(WalletTransactionStatus.FAILED);
                transactionRepository.save(transaction);
                log.error("Withdrawal failed. Transaction: {}, Reference: {}, ResponseCode: {}", 
                        transaction.getCode(), referenceId, responseCode);

                return ApiResponse.<String>builder()
                        .status(HttpStatus.OK.value())
                        .message("Withdrawal failed")
                        .data("FAILED")
                        .build();
            }

        } catch (Exception e) {
            log.error("Error processing VNPay withdrawal callback: {}", e.getMessage(), e);
            return ApiResponse.<String>builder()
                    .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                    .message("Error processing callback")
                    .build();
        }
    }

    /**
     * GET endpoint for VNPay callback (some payment gateways use GET)
     */
    @GetMapping("/callback")
    public ApiResponse<String> handleWithdrawalCallbackGet(@RequestParam Map<String, String> params) {
        return handleWithdrawalCallback(params);
    }
}

