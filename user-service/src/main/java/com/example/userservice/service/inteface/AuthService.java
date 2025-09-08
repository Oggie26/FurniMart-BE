package com.example.userservice.service.inteface;

import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;

public interface AuthService {
    AuthResponse register(RegisterRequest request);
    LoginResponse login(AuthRequest request);
    AuthResponse getUserByUsername(String username);

}
