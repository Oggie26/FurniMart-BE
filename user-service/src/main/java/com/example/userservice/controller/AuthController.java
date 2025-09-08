package com.example.userservice.controller;

import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Validated
@RestController
@RequestMapping("/api/auth")
@SecurityRequirement(name = "api")
@RequiredArgsConstructor
@Tag(name = "Auth Controller")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Đăng nhập", description = "API đăng nhập")
    public ResponseEntity<ApiResponse<LoginResponse>> login(@RequestBody @Valid AuthRequest request) {
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng nhập thành công")
                .data(authService.login(request))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/register")
    @Operation(summary = "Đăng kí", description = "API đăng kí")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<AuthResponse> register(@RequestBody @Valid RegisterRequest registerRequest){

        return ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.CREATED.value())
                .message("Đăng kí thành công")
                .data(authService.register(registerRequest))
                .timestamp(LocalDateTime.now())
                .build();
    }



}
