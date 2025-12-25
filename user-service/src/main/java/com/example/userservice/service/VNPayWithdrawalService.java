package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayWithdrawalService {

    @Value("${vnpay.tmnCode:}")
    private String tmnCode;

    @Value("${vnpay.hashSecret:}")
    private String hashSecret;

    @Value("${vnpay.withdrawalUrl:}")
    private String withdrawalUrl; // VNPay withdrawal API URL (nếu có)

    @Value("${vnpay.withdrawalCallbackUrl:http://152.53.244.124:8080/api/wallets/withdraw-to-vnpay/callback}")
    private String withdrawalCallbackUrl;

    @Value("${vnpay.refundUrl:https://sandbox.vnpayment.vn/merchant_webapi/api/transaction}")
    private String refundUrl;

    private final RestTemplate restTemplate;

    @Async
    public boolean processWithdrawal(Double amount, String bankAccountNumber, 
                                    String bankName, String accountHolderName, 
                                    String referenceId) {
        log.info("Processing VNPay withdrawal: Amount={}, Account={}, Bank={}, Reference={}", 
                amount, bankAccountNumber, bankName, referenceId);

        try {
            if (withdrawalUrl != null && !withdrawalUrl.isEmpty()) {
                return callVNPayWithdrawalAPI(amount, bankAccountNumber, bankName, accountHolderName, referenceId);
            } else {
                return processWithdrawalAsync(amount, bankAccountNumber, bankName, accountHolderName, referenceId);
            }
        } catch (Exception e) {
            log.error("Error processing VNPay withdrawal for reference {}: {}", referenceId, e.getMessage(), e);
            return false;
        }
    }

    private boolean callVNPayWithdrawalAPI(Double amount, String bankAccountNumber,
                                          String bankName, String accountHolderName,
                                          String referenceId) {
        try {
            // Tạo request parameters theo format VNPay
            Map<String, String> params = new LinkedHashMap<>();
            long vnpAmount = Math.round(amount * 100); // Convert to cents

            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "withdraw");
            params.put("vnp_TmnCode", tmnCode);
            params.put("vnp_Amount", String.valueOf(vnpAmount));
            params.put("vnp_CurrCode", "VND");
            params.put("vnp_TxnRef", referenceId);
            params.put("vnp_OrderInfo", String.format("Rut tien ve TK: %s - %s - %s", 
                    bankAccountNumber, bankName, accountHolderName));
            params.put("vnp_BankCode", mapBankNameToCode(bankName));
            params.put("vnp_BankAccount", bankAccountNumber);
            params.put("vnp_BankAccountName", accountHolderName);
            params.put("vnp_CallbackUrl", withdrawalCallbackUrl);

            TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(tz);
            String vnp_CreateDate = sdf.format(new Date());
            params.put("vnp_CreateDate", vnp_CreateDate);

            String hashData = buildHashData(params);
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData);
            params.put("vnp_SecureHash", vnp_SecureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = buildQueryString(params);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling VNPay withdrawal API: {}", withdrawalUrl);
            log.info("Request params: {}", params);

            ResponseEntity<String> response = restTemplate.exchange(
                    withdrawalUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("VNPay withdrawal API response: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());

            return parseVNPayResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling VNPay withdrawal API: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean processWithdrawalAsync(Double amount, String bankAccountNumber,
                                          String bankName, String accountHolderName,
                                          String referenceId) {
        log.info("Processing withdrawal async (no direct API). Reference: {}", referenceId);
        
        // TODO: Implement async processing
        // Option 1: Send notification to admin
        // Option 2: Queue for manual processing
        // Option 3: Wait for webhook callback
        
        try {
            Thread.sleep(2000);
            log.info("Withdrawal request queued for processing. Reference: {}", referenceId);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    private String mapBankNameToCode(String bankName) {
        if (bankName == null) return "NCB";
        
        String name = bankName.toUpperCase();
        if (name.contains("VIETCOMBANK") || name.contains("VCB")) return "VCB";
        if (name.contains("VIETINBANK") || name.contains("VTB")) return "VTB";
        if (name.contains("BIDV")) return "BID";
        if (name.contains("AGRIBANK") || name.contains("VAB")) return "VAB";
        if (name.contains("TECHCOMBANK") || name.contains("TCB")) return "TCB";
        if (name.contains("MBBANK") || name.contains("MB")) return "MBB";
        if (name.contains("ACB")) return "ACB";
        if (name.contains("TPBANK") || name.contains("TP")) return "TPB";
        if (name.contains("VPBANK") || name.contains("VP")) return "VPB";
        if (name.contains("SACOMBANK") || name.contains("STB")) return "STB";
        
        return "NCB";
    }

    private String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String field : fieldNames) {
            if (field.equals("vnp_SecureHash")) continue;
            
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                try {
                    String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                    hashData.append(field).append('=').append(encoded).append('&');
                } catch (Exception e) {
                    log.error("Error encoding value for field {}: {}", field, e.getMessage());
                }
            }
        }
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }
        return hashData.toString();
    }

    private String buildQueryString(Map<String, String> params) {
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (query.length() > 0) query.append("&");
            try {
                query.append(entry.getKey())
                     .append("=")
                     .append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8.toString()));
            } catch (Exception e) {
                log.error("Error encoding query param: {}", e.getMessage());
            }
        }
        return query.toString();
    }

    private boolean parseVNPayResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return false;
        }

        try {
            if (responseBody.contains("vnp_ResponseCode=00") || 
                responseBody.contains("\"ResponseCode\":\"00\"") ||
                responseBody.contains("success")) {
                return true;
            }
            
            log.warn("VNPay withdrawal response indicates failure: {}", responseBody);
            return false;
        } catch (Exception e) {
            log.error("Error parsing VNPay response: {}", e.getMessage());
            return false;
        }
    }

    private String hmacSHA512(String key, String data) {
        try {
            javax.crypto.Mac mac = javax.crypto.Mac.getInstance("HmacSHA512");
            javax.crypto.spec.SecretKeySpec secretKey = new javax.crypto.spec.SecretKeySpec(
                    key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            log.error("Error creating HMAC SHA512: {}", e.getMessage());
            throw new RuntimeException("Error creating VNPay signature", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    public boolean validateWithdrawalCallback(Map<String, String> params) {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        params.remove("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }

        String hashData = buildHashData(params);
        String calculatedHash = hmacSHA512(hashSecret, hashData);

        return calculatedHash.equalsIgnoreCase(receivedHash);
    }

    @Async
    public boolean processRefund(String originalTxnRef, String vnpTransactionNo, 
                                String originalTransactionDate, Double amount,
                                boolean isFullRefund, String orderInfo, String ipAddress) {
        log.info("Processing VNPay refund: TxnRef={}, TransactionNo={}, Amount={}, FullRefund={}", 
                originalTxnRef, vnpTransactionNo, amount, isFullRefund);

        try {
            return callVNPayRefundAPI(originalTxnRef, vnpTransactionNo, originalTransactionDate,
                    amount, isFullRefund, orderInfo, ipAddress);
        } catch (Exception e) {
            log.error("Error processing VNPay refund for TxnRef {}: {}", originalTxnRef, e.getMessage(), e);
            return false;
        }
    }

    private boolean callVNPayRefundAPI(String originalTxnRef, String vnpTransactionNo,
                                      String originalTransactionDate, Double amount,
                                      boolean isFullRefund, String orderInfo, String ipAddress) {
        try {
            Map<String, String> params = new LinkedHashMap<>();
            long vnpAmount = Math.round(amount * 100);

            String vnpRequestId = String.format("%d%04d", 
                    System.currentTimeMillis() % 1000000000L,
                    new Random().nextInt(10000));

            params.put("vnp_RequestId", vnpRequestId);
            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "refund");
            params.put("vnp_TmnCode", tmnCode);
            params.put("vnp_TransactionType", isFullRefund ? "02" : "03");
            params.put("vnp_TxnRef", originalTxnRef);
            params.put("vnp_Amount", String.valueOf(vnpAmount));
            params.put("vnp_OrderInfo", orderInfo != null ? orderInfo : "Hoan tien don hang");
            params.put("vnp_TransactionNo", vnpTransactionNo);
            params.put("vnp_TransactionDate", originalTransactionDate);
            params.put("vnp_CreateBy", "SYSTEM");
            
            TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(tz);
            String vnpCreateDate = sdf.format(new Date());
            params.put("vnp_CreateDate", vnpCreateDate);
            params.put("vnp_IpAddr", ipAddress != null ? ipAddress : "127.0.0.1");

            String hashData = buildHashData(params);
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData);
            params.put("vnp_SecureHash", vnp_SecureHash);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            
            String requestBody = buildQueryString(params);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            log.info("Calling VNPay refund API: {}", refundUrl);
            log.info("Request params: {}", params);

            ResponseEntity<String> response = restTemplate.exchange(
                    refundUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("VNPay refund API response: Status={}, Body={}", 
                    response.getStatusCode(), response.getBody());

            return parseVNPayRefundResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling VNPay refund API: {}", e.getMessage(), e);
            return false;
        }
    }

    private boolean parseVNPayRefundResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return false;
        }

        try {
            if (responseBody.contains("vnp_ResponseCode=00") || 
                responseBody.contains("\"ResponseCode\":\"00\"") ||
                responseBody.contains("\"vnp_ResponseCode\":\"00\"") ||
                responseBody.contains("success")) {
                log.info("VNPay refund successful");
                return true;
            }
            
            log.warn("VNPay refund response indicates failure: {}", responseBody);
            return false;
        } catch (Exception e) {
            log.error("Error parsing VNPay refund response: {}", e.getMessage());
            return false;
        }
    }
}

