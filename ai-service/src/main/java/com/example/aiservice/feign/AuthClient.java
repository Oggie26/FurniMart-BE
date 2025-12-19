package com.example.aiservice.feign;

import com.example.aiservice.config.FeignClientInterceptor;
import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service", contextId = "userClientForInventory", configuration = FeignClientInterceptor.class)
public interface AuthClient {

    @GetMapping("/api/auth/{email}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("email") String email);
}
