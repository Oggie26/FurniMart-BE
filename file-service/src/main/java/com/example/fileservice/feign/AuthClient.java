package com.example.fileservice.feign;

import com.example.fileservice.response.ApiResponse;
import com.example.fileservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service")
public interface AuthClient {

    @GetMapping("/api/auth/{email}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("email") String email);
}
