package com.example.notificationservice.feign;

import com.example.notificationservice.response.ApiResponse;
import com.example.notificationservice.response.UserResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "user-service", contextId = "userClient")
public interface UserClient {

    @GetMapping("/api/users/{id}")
    ApiResponse<UserResponse> getUserById(@PathVariable String id);

    @GetMapping("/api/users/account/{accountId}")
    ApiResponse<UserResponse> getUserByAccountId(@PathVariable String accountId);

}

