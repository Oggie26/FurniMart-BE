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

/**
 * Service để xử lý rút tiền về VNPay
 * 
 * Lưu ý: VNPay không có API trực tiếp để rút tiền về tài khoản ngân hàng.
 * Có 2 cách xử lý:
 * 1. Sử dụng VNPay Gateway API (nếu có merchant account với quyền withdrawal)
 * 2. Xử lý thủ công: Tạo yêu cầu, admin xử lý, update status qua webhook
 * 
 * Implementation này hỗ trợ cả 2 cách.
 */
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

    private final RestTemplate restTemplate;

    /**
     * Xử lý rút tiền qua VNPay API
     * 
     * @param amount Số tiền rút
     * @param bankAccountNumber Số tài khoản ngân hàng
     * @param bankName Tên ngân hàng
     * @param accountHolderName Tên chủ tài khoản
     * @param referenceId Reference ID để tracking
     * @return true nếu thành công, false nếu thất bại
     */
    @Async
    public boolean processWithdrawal(Double amount, String bankAccountNumber, 
                                    String bankName, String accountHolderName, 
                                    String referenceId) {
        log.info("Processing VNPay withdrawal: Amount={}, Account={}, Bank={}, Reference={}", 
                amount, bankAccountNumber, bankName, referenceId);

        try {
            // Nếu có VNPay withdrawal API URL, gọi API trực tiếp
            if (withdrawalUrl != null && !withdrawalUrl.isEmpty()) {
                return callVNPayWithdrawalAPI(amount, bankAccountNumber, bankName, accountHolderName, referenceId);
            } else {
                // Nếu không có API, xử lý async (giả lập hoặc chờ webhook)
                return processWithdrawalAsync(amount, bankAccountNumber, bankName, accountHolderName, referenceId);
            }
        } catch (Exception e) {
            log.error("Error processing VNPay withdrawal for reference {}: {}", referenceId, e.getMessage(), e);
            return false;
        }
    }

    /**
     * Gọi VNPay API trực tiếp (nếu có)
     */
    private boolean callVNPayWithdrawalAPI(Double amount, String bankAccountNumber,
                                          String bankName, String accountHolderName,
                                          String referenceId) {
        try {
            // Tạo request parameters theo format VNPay
            Map<String, String> params = new LinkedHashMap<>();
            long vnpAmount = Math.round(amount * 100); // Convert to cents

            params.put("vnp_Version", "2.1.0");
            params.put("vnp_Command", "withdraw"); // VNPay withdrawal command
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

            // Thời gian
            TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
            SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
            sdf.setTimeZone(tz);
            String vnp_CreateDate = sdf.format(new Date());
            params.put("vnp_CreateDate", vnp_CreateDate);

            // Tạo hash
            String hashData = buildHashData(params);
            String vnp_SecureHash = hmacSHA512(hashSecret, hashData);
            params.put("vnp_SecureHash", vnp_SecureHash);

            // Gọi VNPay API
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

            // Parse response (tùy format VNPay trả về)
            return parseVNPayResponse(response.getBody());

        } catch (Exception e) {
            log.error("Error calling VNPay withdrawal API: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Xử lý async (không có API trực tiếp)
     * Có thể:
     * 1. Gửi request đến admin để xử lý thủ công
     * 2. Chờ webhook callback từ VNPay
     * 3. Hoặc giả lập xử lý (cho testing)
     */
    private boolean processWithdrawalAsync(Double amount, String bankAccountNumber,
                                          String bankName, String accountHolderName,
                                          String referenceId) {
        log.info("Processing withdrawal async (no direct API). Reference: {}", referenceId);
        
        // TODO: Implement async processing
        // Option 1: Send notification to admin
        // Option 2: Queue for manual processing
        // Option 3: Wait for webhook callback
        
        // For now, simulate processing (remove in production)
        try {
            Thread.sleep(2000); // Simulate processing time
            log.info("Withdrawal request queued for processing. Reference: {}", referenceId);
            // In production, this should queue the request and wait for webhook
            return true; // Return true to indicate request accepted (not completed)
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Map tên ngân hàng sang bank code của VNPay
     */
    private String mapBankNameToCode(String bankName) {
        if (bankName == null) return "NCB";
        
        String name = bankName.toUpperCase();
        // Map common bank names to VNPay bank codes
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
        
        return "NCB"; // Default: NCB
    }

    /**
     * Build hash data string for VNPay
     */
    private String buildHashData(Map<String, String> params) {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String field : fieldNames) {
            if (field.equals("vnp_SecureHash")) continue; // Skip secure hash
            
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

    /**
     * Build query string from params
     */
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

    /**
     * Parse VNPay API response
     */
    private boolean parseVNPayResponse(String responseBody) {
        if (responseBody == null || responseBody.isEmpty()) {
            return false;
        }

        try {
            // VNPay thường trả về JSON hoặc query string
            // Parse response code
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

    /**
     * HMAC SHA512 hash
     */
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

    /**
     * Validate webhook callback from VNPay
     */
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
}

