package com.example.userservice.controller;

import com.example.userservice.config.JwtService;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.ForgotPasswordRequest;
import com.example.userservice.request.GoogleLoginRequest;
import com.example.userservice.request.RefreshTokenRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.request.ResetPasswordRequest;
import com.example.userservice.request.TokenRequest;
import com.example.userservice.request.VerifyEmailRequest;
import com.example.userservice.request.VerifyOtpRequest;
import com.example.userservice.response.ApiResponse;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.GoogleOAuth2Service;
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
    private final GoogleOAuth2Service googleOAuth2Service;

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

    @PostMapping("/google/login")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Đăng nhập với Google", description = "API đăng nhập bằng Google OAuth2")
    public ResponseEntity<ApiResponse<LoginResponse>> googleLogin(@RequestBody @Valid GoogleLoginRequest request) {
        return ResponseEntity.ok(ApiResponse.<LoginResponse>builder()
                .status(HttpStatus.OK.value())
                .message("Đăng nhập với Google thành công")
                .data(googleOAuth2Service.authenticateWithGoogle(request.getAccessToken()))
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Quên mật khẩu", description = "API gửi OTP và reset token qua email để đặt lại mật khẩu")
    public ResponseEntity<ApiResponse<String>> forgotPassword(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đã gửi mã OTP và link đặt lại mật khẩu qua email")
                .data("OTP và reset link đã được gửi đến email của bạn")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Đặt lại mật khẩu", description = "API đặt lại mật khẩu bằng reset token")
    public ResponseEntity<ApiResponse<String>> resetPassword(@RequestBody @Valid ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đặt lại mật khẩu thành công")
                .data("Mật khẩu đã được đặt lại thành công")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Xác thực email", description = "API xác thực email bằng verification token")
    public ResponseEntity<ApiResponse<String>> verifyEmail(@RequestBody @Valid VerifyEmailRequest request) {
        authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Xác thực email thành công")
                .data("Email đã được xác thực thành công")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/resend-verification-email")
    @Operation(summary = "Gửi lại email xác thực", description = "API gửi lại email xác thực")
    public ResponseEntity<ApiResponse<String>> resendVerificationEmail(@RequestBody @Valid ForgotPasswordRequest request) {
        authService.resendVerificationEmail(request.getEmail());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("Đã gửi lại email xác thực")
                .data("Email xác thực đã được gửi lại đến email của bạn")
                .timestamp(LocalDateTime.now())
                .build());
    }

    @PostMapping("/verify-otp")
    @Operation(summary = "Xác thực OTP cho reset password", description = "API xác thực OTP code trước khi reset password")
    public ResponseEntity<ApiResponse<String>> verifyOtp(@RequestParam String email, @RequestBody @Valid VerifyOtpRequest request) {
        authService.verifyOtpForPasswordReset(email, request.getOtpCode());
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .status(HttpStatus.OK.value())
                .message("OTP xác thực thành công")
                .data("OTP đã được xác thực. Bạn có thể đặt lại mật khẩu.")
                .timestamp(LocalDateTime.now())
                .build());
    }

}
