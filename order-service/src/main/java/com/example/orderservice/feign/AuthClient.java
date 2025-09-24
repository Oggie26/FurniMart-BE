package com.example.orderservice.feign;

import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service", contextId = "authClient")
public interface AuthClient {

    @GetMapping("/api/auth/{email}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("email") String email);


}
