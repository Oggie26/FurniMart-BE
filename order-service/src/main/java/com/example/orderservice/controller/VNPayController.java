package com.example.orderservice.controller;

import com.example.orderservice.service.VNPayService;
import com.example.orderservice.util.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private final VNPayService vnPayService;

    @GetMapping("/vnpay")
    public String createPayment(@RequestParam Double amount,
                                @RequestParam Long orderId) throws Exception {
        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return";
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> vnpParams) {
        Map<String, String> params = new HashMap<>(vnpParams); // clone để an toàn

        try {
            boolean valid = vnPayService.validateCallback(params);
            if (!valid) {
                log.error("Invalid VNPay signature: received={}", vnpParams.get("vnp_SecureHash"));
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", 400,
                        "message", "Invalid VNPay signature"
                ));
            }

            String responseCode = params.get("vnp_ResponseCode");
            String orderId = params.get("vnp_TxnRef");

            if ("00".equals(responseCode)) {
                return ResponseEntity.ok(Map.of(
                        "status", 200,
                        "message", "Payment success",
                        "orderId", orderId
                ));
            } else {
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                        "status", 400,
                        "message", "Payment failed with code: " + responseCode,
                        "orderId", orderId
                ));
            }
        } catch (UnsupportedEncodingException e) {
            log.error("Error validating VNPay callback", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "status", 500,
                    "message", "Server error when validating signature"
            ));
        }
    }



}
