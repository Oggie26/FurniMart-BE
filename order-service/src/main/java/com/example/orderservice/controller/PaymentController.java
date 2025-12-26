package com.example.orderservice.controller;

import com.example.orderservice.entity.Payment;
import com.example.orderservice.feign.UserClient;
import com.example.orderservice.repository.PaymentRepository;
import com.example.orderservice.request.VoucherRequest;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.PaymentAdminResponse;
import com.example.orderservice.response.UserResponse;
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
        public ApiResponse<List<PaymentAdminResponse>> getAllTransactionPayment() {
                List<Payment> payments = paymentRepository.findAll();
                List<PaymentAdminResponse> responses = payments.stream()
                                .map(payment -> {
                                        UserResponse user = null;
                                        if (payment.getUserId() != null) {
                                                try {
                                                        ApiResponse<UserResponse> userApiResponse = userClient
                                                                        .getUserById(payment.getUserId());
                                                        if (userApiResponse != null
                                                                        && userApiResponse.getData() != null) {
                                                                user = userApiResponse.getData();
                                                        }
                                                } catch (Exception e) {
                                                        log.error("Error fetching user {} from user-service: {}",
                                                                        payment.getUserId(), e.getMessage());
                                                }
                                        }

                                        return PaymentAdminResponse.builder()
                                                        .id(payment.getId())
                                                        .transactionCode(payment.getTransactionCode())
                                                        .total(payment.getTotal())
                                                        .paymentMethod(payment.getPaymentMethod())
                                                        .paymentStatus(payment.getPaymentStatus())
                                                        .date(payment.getDate())
                                                        .userId(payment.getUserId())
                                                        .userName(user != null ? user.getFullName() : null)
                                                        .email(user != null ? user.getEmail() : null)
                                                        .build();
                                })
                                .sorted((p1, p2) -> p2.getDate() != null && p1.getDate() != null
                                                ? p2.getDate().compareTo(p1.getDate())
                                                : 0)
                                .collect(java.util.stream.Collectors.toList());

                return ApiResponse.<List<PaymentAdminResponse>>builder()
                                .status(HttpStatus.OK.value())
                                .message("Get transaction successfully")
                                .data(responses)
                                .build();
        }

}
