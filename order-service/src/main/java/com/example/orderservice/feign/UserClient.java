package com.example.orderservice.feign;

import com.example.orderservice.config.FeignClientInterceptor;
import com.example.orderservice.response.AddressResponse;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.UserResponse;

import com.example.orderservice.response.WalletResponse;
import io.swagger.v3.oas.annotations.Operation;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "user-service", configuration = FeignClientInterceptor.class)
public interface UserClient {

        @GetMapping("/api/addresses/{id}")
        ApiResponse<AddressResponse> getAddressById(@PathVariable Long id);

        @GetMapping("/api/users/{id}")
        ApiResponse<UserResponse> getUserById(@PathVariable String id);

        @GetMapping("/api/users/account/{accountId}")
        ApiResponse<UserResponse> getUserByAccountId(@PathVariable String accountId);

        @GetMapping("/api/employees/account/{accountId}")
        ApiResponse<UserResponse> getEmployeeByAccountId(@PathVariable String accountId);

        @GetMapping("/api/users/count")
        ApiResponse<Long> getTotalUsersCount();

        @PostMapping("/api/wallets/{walletId}/refund")
        ApiResponse<WalletResponse> refundToWallet(
                        @PathVariable("walletId") String walletId,
                        @RequestParam("amount") Double amount,
                        @RequestParam(value = "description", required = false) String description,
                        @RequestParam(value = "referenceId", required = false) String referenceId);

        @GetMapping("/api/wallets/user/{userId}")
        ApiResponse<WalletResponse> getWalletByUserId(@PathVariable String userId);

        @PostMapping("/api/wallets/{walletId}/refund-to-vnpay")
        ApiResponse<WalletResponse> refundToVNPay(
                        @PathVariable String walletId,
                        @RequestParam Double amount,
                        @RequestParam(required = false) String description,
                        @RequestParam(required = false) Long orderId);
}
