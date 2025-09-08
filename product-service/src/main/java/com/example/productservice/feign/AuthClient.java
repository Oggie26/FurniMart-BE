package com.example.productservice.feign;

import com.example.productservice.response.ApiResponse;
import com.example.productservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service")
public interface AuthClient {

    @GetMapping("/api/auth/{username}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("username") String username);
}
