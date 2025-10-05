package com.example.orderservice.controller;

import com.example.orderservice.service.VNPayService;
import com.example.orderservice.util.VNPayUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class VNPayController {

    @Value("${vnpay.hashSecret}")
    private String hashSecret;

    private final VNPayService vnPayService;

    @GetMapping("/vnpay")
    public String createPayment(@RequestParam Double amount,
                                @RequestParam Long orderId) throws Exception {
        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return"; // ✅ backend URL
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

    @GetMapping("/vnpay-return")
    public ResponseEntity<?> vnpayReturn(@RequestParam Map<String, String> vnpParams) {
        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);

        if (signValue.equals(secureHash)) {
            String responseCode = vnpParams.get("vnp_ResponseCode");
            if ("00".equals(responseCode)) {
                String orderId = vnpParams.get("vnp_TxnRef");
                return ResponseEntity.ok(Map.of(
                        "status", 200,
                        "message", "✅ Payment Success",
                        "orderId", orderId
                ));
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                        "status", 400,
                        "message", "❌ Payment Failed: " + responseCode
                ));
            }
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of(
                    "status", 400,
                    "message", "❌ Invalid signature"
            ));
        }
    }

}
