package com.example.userservice.controller;

import com.example.userservice.config.JwtService;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RefreshTokenRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.request.TokenRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import io.jsonwebtoken.JwtException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
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
    private final JwtService jwtService;

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

    @GetMapping("/{email}")
    public ApiResponse<AuthResponse> getUserByUsername(@PathVariable String email) {
        AuthResponse authResponse = authService.getUserByUsername(email);
        return ApiResponse.<AuthResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Lấy thông tin người dùng thành công")
                .data(authResponse)
                .timestamp(LocalDateTime.now())
                .build();
    }

    @PostMapping("/logout")
    @SecurityRequirement(name = "bearerAuth")
    @Operation(summary = "Đăng xuất", description = "API đăng xuất và blacklist token")
    public ResponseEntity<ApiResponse<String>> logout(@RequestBody TokenRequest tokenRequest) {
        authService.logout(tokenRequest.getToken());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng xuất thành công")
                .data("Logged out successfully")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Token", description = "API tạo access token mới từ refresh token")
    public ResponseEntity<ApiResponse<LoginResponse>> refreshToken(@RequestBody @Valid RefreshTokenRequest request) {
        LoginResponse loginResponse = authService.refreshToken(request.getRefreshToken());
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Refresh token thành công")
                .data(loginResponse)
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-token")
    public ResponseEntity<Void> verifyToken(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = authorizationHeader.substring(7);
        try {
            jwtService.validateToken(token);
            return ResponseEntity.ok().build();
        } catch (JwtException | IllegalArgumentException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }

}
