package com.example.userservice.controller;

import com.example.userservice.entity.Wallet;
import com.example.userservice.entity.WalletTransaction;
import com.example.userservice.enums.WalletTransactionStatus;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.repository.WalletTransactionRepository;
import com.example.userservice.service.VNPayDepositService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;

/**
 * Controller để xử lý callback từ VNPay cho deposit
 */
@RestController
@RequestMapping("/api/wallets/deposit-via-vnpay")
@RequiredArgsConstructor
@Slf4j
public class WalletDepositController {

    private final VNPayDepositService vnPayDepositService;
    private final WalletTransactionRepository transactionRepository;
    private final WalletRepository walletRepository;

    /**
     * Callback từ VNPay sau khi user thanh toán
     * VNPay sẽ redirect về endpoint này với các tham số payment result
     */
    @GetMapping("/callback")
    public void depositCallback(
            @RequestParam Map<String, String> params,
            @RequestHeader(value = "User-Agent", required = false) String userAgent,
            HttpServletResponse response) throws IOException {

        response.setContentType("text/html; charset=UTF-8");

        log.info("Received VNPay deposit callback: {}", params);

        String transactionId = params.get("vnp_TxnRef");
        String responseCode = params.get("vnp_ResponseCode");
        String transactionNo = params.get("vnp_TransactionNo");

        if (transactionId == null || transactionId.isEmpty()) {
            transactionId = "unknown";
        }

        String webUrl = "http://152.53.244.124:8080/wallet/deposit-success";
        String mobileDeepLink = "furnimartmobileapp://wallet-deposit-success";

        boolean isMobile = userAgent != null && (
                userAgent.toLowerCase().contains("android") ||
                userAgent.toLowerCase().contains("iphone") ||
                userAgent.toLowerCase().contains("mobile")
        );

        // Validate signature
        boolean isValidSignature = false;
        try {
            isValidSignature = vnPayDepositService.validateCallback(params);
        } catch (Exception e) {
            log.error("Error validating VNPay callback signature: {}", e.getMessage(), e);
        }

        if (!isValidSignature) {
            log.error("Invalid VNPay deposit callback signature for transaction: {}", transactionId);
            String errorHtml = buildErrorHtml("Giao dịch không hợp lệ", "Chữ ký không khớp.", 
                    isMobile ? mobileDeepLink + "?status=invalid" : webUrl + "?status=invalid", isMobile);
            response.getWriter().write(errorHtml);
            return;
        }

        // Find transaction
        WalletTransaction transaction = transactionRepository.findByIdAndIsDeletedFalse(transactionId)
                .orElse(null);

        if (transaction == null) {
            log.error("Transaction not found: {}", transactionId);
            String errorHtml = buildErrorHtml("Giao dịch không tìm thấy", 
                    "Không tìm thấy giao dịch với ID: " + transactionId,
                    isMobile ? mobileDeepLink + "?status=notfound" : webUrl + "?status=notfound", isMobile);
            response.getWriter().write(errorHtml);
            return;
        }

        // Check if already processed
        if (transaction.getStatus() == WalletTransactionStatus.COMPLETED) {
            log.warn("Transaction {} already completed", transactionId);
            String successHtml = buildSuccessHtml("Nạp tiền thành công", 
                    "Giao dịch đã được xử lý trước đó.",
                    transaction.getCode(),
                    isMobile ? mobileDeepLink + "?status=success&transactionId=" + transactionId : 
                             webUrl + "?status=success&transactionId=" + transactionId, isMobile);
            response.getWriter().write(successHtml);
            return;
        }

        // Process payment result
        if ("00".equals(responseCode)) {
            // Payment successful
            try {
                // Update transaction status
                transaction.setStatus(WalletTransactionStatus.COMPLETED);
                transaction.setDescription("Nạp tiền qua VNPay - Thành công. Transaction No: " + transactionNo);
                transactionRepository.save(transaction);

                // Deposit to wallet
                Wallet wallet = walletRepository.findByIdAndIsDeletedFalse(transaction.getWalletId())
                        .orElse(null);

                if (wallet != null) {
                    BigDecimal balanceBefore = wallet.getBalance();
                    BigDecimal balanceAfter = balanceBefore.add(transaction.getAmount());
                    wallet.setBalance(balanceAfter);
                    walletRepository.save(wallet);

                    // Update transaction balance after
                    transaction.setBalanceAfter(balanceAfter);
                    transactionRepository.save(transaction);

                    log.info("Deposit successful: Transaction {}, Amount {}, Wallet {}, Balance {} -> {}",
                            transaction.getCode(), transaction.getAmount(), wallet.getId(), 
                            balanceBefore, balanceAfter);

                    String successHtml = buildSuccessHtml("Nạp tiền thành công!", 
                            String.format("Đã nạp %s VND vào wallet.", 
                                    transaction.getAmount().longValue()),
                            transaction.getCode(),
                            isMobile ? mobileDeepLink + "?status=success&transactionId=" + transactionId : 
                                     webUrl + "?status=success&transactionId=" + transactionId, isMobile);
                    response.getWriter().write(successHtml);
                } else {
                    log.error("Wallet not found: {}", transaction.getWalletId());
                    transaction.setStatus(WalletTransactionStatus.FAILED);
                    transaction.setDescription("Nạp tiền qua VNPay - Thất bại: Wallet không tồn tại");
                    transactionRepository.save(transaction);
                    
                    String errorHtml = buildErrorHtml("Nạp tiền thất bại", 
                            "Wallet không tồn tại.",
                            isMobile ? mobileDeepLink + "?status=failed" : webUrl + "?status=failed", isMobile);
                    response.getWriter().write(errorHtml);
                }
            } catch (Exception e) {
                log.error("Error processing deposit callback: {}", e.getMessage(), e);
                transaction.setStatus(WalletTransactionStatus.FAILED);
                transaction.setDescription("Nạp tiền qua VNPay - Thất bại: " + e.getMessage());
                transactionRepository.save(transaction);
                
                String errorHtml = buildErrorHtml("Nạp tiền thất bại", 
                        "Có lỗi xảy ra khi xử lý giao dịch.",
                        isMobile ? mobileDeepLink + "?status=failed" : webUrl + "?status=failed", isMobile);
                response.getWriter().write(errorHtml);
            }
        } else {
            // Payment failed
            log.warn("VNPay payment failed for transaction {}: ResponseCode = {}", transactionId, responseCode);
            transaction.setStatus(WalletTransactionStatus.FAILED);
            transaction.setDescription("Nạp tiền qua VNPay - Thất bại. ResponseCode: " + responseCode);
            transactionRepository.save(transaction);

            String errorHtml = buildErrorHtml("Nạp tiền thất bại", 
                    "Mã lỗi: " + responseCode,
                    isMobile ? mobileDeepLink + "?status=failed&code=" + responseCode : 
                             webUrl + "?status=failed&code=" + responseCode, isMobile);
            response.getWriter().write(errorHtml);
        }
    }

    private String buildSuccessHtml(String title, String message, String transactionCode, String redirectUrl, boolean isMobile) {
        return String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s</title>
    <style>
        body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
        .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }
        h1 { color: #3B6C46; margin: 0 0 16px; font-size: 24px; }
        p { color: #555; margin: 0 0 24px; font-size: 16px; }
        .code { color: #666; font-size: 14px; margin: 10px 0; }
        .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
        .close-btn { position: absolute; top: 10px; right: 10px; background: none; border: none; font-size: 28px; cursor: pointer; color: #999; font-weight: bold; }
        .close-btn:hover { color: #333; }
    </style>
</head>
<body>
    <div class="container">
        <button class="close-btn" onclick="window.location.href='%s'">×</button>
        <h1>%s</h1>
        <p>%s</p>
        <div class="code">Mã giao dịch: %s</div>
        <a href="%s" class="btn">Quay lại ứng dụng</a>
    </div>
</body>
</html>
""", title, redirectUrl, title, message, transactionCode, redirectUrl);
    }

    private String buildErrorHtml(String title, String message, String redirectUrl, boolean isMobile) {
        return String.format("""
<!DOCTYPE html>
<html lang="vi">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>%s</title>
    <style>
        body { font-family: -apple-system, sans-serif; background: #f8f9fa; margin: 0; padding: 40px; text-align: center; }
        .container { max-width: 400px; margin: 0 auto; background: white; padding: 30px; border-radius: 16px; box-shadow: 0 4px 12px rgba(0,0,0,0.1); position: relative; }
        h1 { color: #dc3545; margin: 0 0 16px; font-size: 24px; }
        p { color: #555; margin: 0 0 24px; font-size: 16px; }
        .btn { background: #3B6C46; color: white; padding: 12px 24px; border-radius: 12px; text-decoration: none; display: inline-block; font-weight: 600; font-size: 16px; }
        .close-btn { position: absolute; top: 10px; right: 10px; background: none; border: none; font-size: 28px; cursor: pointer; color: #999; }
    </style>
</head>
<body>
    <div class="container">
        <button class="close-btn" onclick="window.location.href='%s'">×</button>
        <h1>%s</h1>
        <p>%s</p>
        <a href="%s" class="btn">Quay lại ứng dụng</a>
    </div>
</body>
</html>
""", title, redirectUrl, title, message, redirectUrl);
    }
}

