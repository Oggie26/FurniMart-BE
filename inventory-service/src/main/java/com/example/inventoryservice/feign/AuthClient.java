package com.example.inventoryservice.feign;

import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.AuthResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;


@FeignClient(name = "user-service", contextId = "userClientForInventory")
public interface AuthClient {

    @GetMapping("/api/auth/{email}")
    ApiResponse<AuthResponse> getUserByUsername(@PathVariable("email") String email);
}
