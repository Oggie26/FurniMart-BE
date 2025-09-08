package com.example.userservice.feign;

import com.example.userservice.request.AuthRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "auth-service")
public interface AuthClient {

    @PostMapping("/api/auth/register")
    ApiResponse<AuthResponse> register(@RequestBody AuthRequest authRequest);

    @GetMapping("/api/auth/{username}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("username") String username);

    @PatchMapping("/api/auth/changePassword")
    ApiResponse<AuthResponse> changePassword(@RequestBody String changePassword);
}
