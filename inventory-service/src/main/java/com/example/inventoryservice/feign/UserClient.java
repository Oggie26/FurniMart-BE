package com.example.inventoryservice.feign;

import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.StaffResponse;
import com.example.inventoryservice.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {


    @GetMapping("/api/users/{id}")
    ApiResponse<UserResponse> getUserById(@PathVariable String id);

    @GetMapping("/api/employees/account/{accountId}")
    ApiResponse<UserResponse> getEmployeeByAccountId(@PathVariable String accountId);

}

