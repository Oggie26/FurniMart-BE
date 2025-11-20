package com.example.userservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Service để tạo VNPay payment URL cho nạp tiền vào wallet
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class VNPayDepositService {

    @Value("${vnpay.tmnCode}")
    private String tmnCode;

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    @Value("${vnpay.url}")
    private String vnpUrl;

    @Value("${vnpay.depositCallbackUrl:http://152.53.244.124:8086/api/wallets/deposit-via-vnpay/callback}")
    private String depositCallbackUrl;

    /**
     * Tạo VNPay payment URL cho nạp tiền vào wallet
     * 
     * @param transactionId ID của transaction (dùng làm vnp_TxnRef)
     * @param amount Số tiền nạp (VND)
     * @param ipAddress IP address của client
     * @return VNPay payment URL
     */
    public String createDepositPaymentUrl(String transactionId, Double amount, String ipAddress) throws Exception {
        if (amount == null || amount <= 0) {
            throw new IllegalArgumentException("Số tiền phải > 0");
        }

        Map<String, String> params = new LinkedHashMap<>();
        long vnpAmount = Math.round(amount * 100); // Convert to cents

        params.put("vnp_Version", "2.1.0");
        params.put("vnp_Command", "pay");
        params.put("vnp_TmnCode", tmnCode);
        params.put("vnp_Amount", String.valueOf(vnpAmount));
        params.put("vnp_CurrCode", "VND");
        params.put("vnp_TxnRef", transactionId);
        params.put("vnp_OrderInfo", "Nap tien vao wallet - Transaction: " + transactionId);
        params.put("vnp_OrderType", "other");
        params.put("vnp_Locale", "vn");
        params.put("vnp_ReturnUrl", depositCallbackUrl);
        params.put("vnp_IpAddr", ipAddress);

        // Thời gian Việt Nam
        TimeZone tz = TimeZone.getTimeZone("Asia/Ho_Chi_Minh");
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmss");
        sdf.setTimeZone(tz);
        Date now = new Date();
        String vnp_CreateDate = sdf.format(now);

        Calendar expireCal = Calendar.getInstance(tz);
        expireCal.setTime(now);
        expireCal.add(Calendar.MINUTE, 15);
        String vnp_ExpireDate = sdf.format(expireCal.getTime());

        params.put("vnp_CreateDate", vnp_CreateDate);
        params.put("vnp_ExpireDate", vnp_ExpireDate);

        // === BƯỚC 1: TẠO hashData ===
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String field : fieldNames) {
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                hashData.append(field).append('=').append(encoded).append('&');
            }
        }
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        // === BƯỚC 2: TẠO vnp_SecureHash ===
        String vnp_SecureHash = hmacSHA512(hashSecret, hashData.toString());

        // === BƯỚC 3: TẠO URL ===
        String paymentUrl = buildSafeQueryUrl(vnpUrl, params, vnp_SecureHash);

        log.info("=== VNPAY DEPOSIT URL CREATED ===");
        log.info("Transaction ID: {}", transactionId);
        log.info("Amount: {} VND → vnp_Amount: {}", amount, vnpAmount);
        log.info("CreateDate: {}", vnp_CreateDate);
        log.info("ExpireDate: {}", vnp_ExpireDate);
        log.info("IP: {}", ipAddress);
        log.info("ReturnUrl: {}", depositCallbackUrl);
        log.info("Payment URL: {}", paymentUrl);
        log.info("================================");

        return paymentUrl;
    }

    /**
     * Tạo URL an toàn với vnp_SecureHash không bị encode
     */
    private String buildSafeQueryUrl(String baseUrl, Map<String, String> params, String secureHash) throws Exception {
        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder query = new StringBuilder();
        for (String field : fieldNames) {
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                if (query.length() > 0) query.append("&");
                query.append(field).append("=").append(encoded);
            }
        }
        if (query.length() > 0) {
            query.append("&");
        }
        query.append("vnp_SecureHash=").append(secureHash); // GIỮ NGUYÊN

        return baseUrl + "?" + query.toString();
    }

    /**
     * HMAC SHA512 hash
     */
    private String hmacSHA512(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA512");
            SecretKeySpec secretKey = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA512");
            mac.init(secretKey);
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new RuntimeException("Lỗi tạo chữ ký VNPay", e);
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
     * Validate callback từ VNPay
     */
    public boolean validateCallback(Map<String, String> params) throws Exception {
        String receivedHash = params.get("vnp_SecureHash");
        if (receivedHash == null || receivedHash.isEmpty()) {
            return false;
        }

        params.remove("vnp_SecureHash");
        if (params.containsKey("vnp_SecureHashType")) {
            params.remove("vnp_SecureHashType");
        }

        List<String> fieldNames = new ArrayList<>(params.keySet());
        Collections.sort(fieldNames);

        StringBuilder hashData = new StringBuilder();
        for (String field : fieldNames) {
            String value = params.get(field);
            if (value != null && !value.isEmpty()) {
                String encoded = URLEncoder.encode(value, StandardCharsets.UTF_8.toString());
                hashData.append(field).append('=').append(encoded).append('&');
            }
        }
        if (hashData.length() > 0) {
            hashData.deleteCharAt(hashData.length() - 1);
        }

        String calculatedHash = hmacSHA512(hashSecret, hashData.toString());
        return calculatedHash.equalsIgnoreCase(receivedHash);
    }
}

