package com.example.orderservice.controller;

import com.example.orderservice.service.VNPayService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;

    @GetMapping("/vnpay")
    public String createPayment(@RequestParam Double amount,
                                @RequestParam Long orderId) throws Exception {
        String returnUrl = "http://localhost:8085/api/v1/payment/vnpay-return";
        return vnPayService.createPaymentUrl(orderId, amount, returnUrl);
    }

    @GetMapping("/vnpay-return")
    public String vnpayReturn(@RequestParam Map<String, String> allParams) {
        // Khi thanh toán xong VNPay sẽ redirect về đây kèm query param
        return "VNPay Response: " + allParams.toString();
    }
}
