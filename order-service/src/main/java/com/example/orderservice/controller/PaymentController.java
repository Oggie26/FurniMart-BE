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

        @PostMapping
        @Operation(summary = "Get all payment")
        @ResponseStatus(HttpStatus.CREATED)
        public ApiResponse<List<Payment>> getAllTransactionPayment() {
                return ApiResponse.<List<Payment>>builder()
                        .status(HttpStatus.CREATED.value())
                        .message("Get transaction successfully")
                        .data(paymentRepository.findAll())
                        .build();
        }


}
