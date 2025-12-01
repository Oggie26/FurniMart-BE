package com.example.orderservice.feign;

import com.example.orderservice.config.FeignClientInterceptor;
import com.example.orderservice.response.AddressResponse;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
@FeignClient(
        name = "user-service",
        configuration = FeignClientInterceptor.class
)
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
}




