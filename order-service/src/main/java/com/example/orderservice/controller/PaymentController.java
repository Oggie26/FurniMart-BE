package com.example.orderservice.controller;

import com.example.orderservice.entity.Payment;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.request.VoucherRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.PaymentResponse;
import com.example.orderservice.response.VoucherResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/payments")
@Tag(name = "Payment & Escrow Controller")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@Slf4j
public class PaymentController {

        private final UserClient userClient;
        private final PaymentRepository paymentRepository;

        @GetMapping
        @Operation(summary = "Get all payments")
        @PreAuthorize("hasRole('ADMIN') or hasRole('STAFF')")
        public ApiResponse<List<PaymentResponse>> getAllTransactionPayment() {
                List<Payment> payments = paymentRepository.findAll();
                List<PaymentResponse> responses = payments.stream()
                                .map(payment -> PaymentResponse.builder()
                                                .id(payment.getId())
                                                .transactionCode(payment.getTransactionCode())
                                                .total(payment.getTotal())
                                                .paymentMethod(payment.getPaymentMethod())
                                                .paymentStatus(payment.getPaymentStatus())
                                                .date(payment.getDate())
                                                .build())
                                .collect(java.util.stream.Collectors.toList());

                return ApiResponse.<List<PaymentResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Get transaction successfully")
                                .data(responses)
                                .build();
        }

}
