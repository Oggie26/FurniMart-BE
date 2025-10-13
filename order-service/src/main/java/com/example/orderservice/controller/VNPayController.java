package com.example.orderservice.controller;

import com.example.orderservice.entity.Order;
import com.example.orderservice.enums.ErrorCode;
import com.example.orderservice.event.OrderCreatedEvent;
import com.example.orderservice.exception.AppException;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.OrderRepository;
import com.example.orderservice.service.VNPayService;
import com.example.orderservice.util.VNPayUtils;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payment")
@RequiredArgsConstructor
@Slf4j
public class VNPayController {

    private final OrderRepository orderRepository;
    private final UserClient userClient;

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
    public void vnpayReturn(@RequestParam Map<String, String> vnpParams,
                            HttpServletResponse response) throws IOException {

        String secureHash = vnpParams.remove("vnp_SecureHash");
        vnpParams.remove("vnp_SecureHashType");

        String signValue = VNPayUtils.hashAllFields(vnpParams, hashSecret);

        String frontendUrl = "http://localhost:5173/payment-success";

        if (signValue.equalsIgnoreCase(secureHash)) {
            String responseCode = vnpParams.get("vnp_ResponseCode");
            String orderId = vnpParams.get("vnp_TxnRef");

            if ("00".equals(responseCode)) {
                Order order = orderRepository.findById(Long.parseLong(orderId))
                                .orElseThrow(() -> new AppException(ErrorCode.ORDER_NOT_FOUND));
                response.sendRedirect(frontendUrl + "?status=success&orderId=" + URLEncoder.encode(orderId, StandardCharsets.UTF_8));
            } else {
                response.sendRedirect(frontendUrl + "?status=failed&code=" + URLEncoder.encode(responseCode, StandardCharsets.UTF_8));
            }
        } else {
            response.sendRedirect(frontendUrl + "?status=invalid");
        }
    }


}
