package com.example.userservice.service.inteface;

import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.ForgotPasswordRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.request.ResetPasswordRequest;
import com.example.userservice.request.VerifyEmailRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    LoginResponse login(AuthRequest request);
    AuthResponse getUserByUsername(String email);
    void logout(String token);
    LoginResponse refreshToken(String refreshToken);
    void forgotPassword(ForgotPasswordRequest request);
    void resetPassword(ResetPasswordRequest request);
    void verifyEmail(VerifyEmailRequest request);
    void resendVerificationEmail(String email);
    void verifyOtpForPasswordReset(String email, String otpCode);
}
