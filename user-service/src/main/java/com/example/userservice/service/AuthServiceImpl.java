package com.example.userservice.service;

import com.example.userservice.config.JwtService;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getUsername() == null || request.getPassword() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getPassword().length() < 6) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        if (request.getUsername().length() < 6) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        User user = User.builder()
                .username(request.getUsername())
                .password(passwordEncoder.encode(request.getPassword()))
                .email(request.getEmail())
                .phone(request.getPhone())
                .status(EnumStatus.ACTIVE)
                .birthday(request.getBirthDay())
                .gender(request.getGender())
                .fullName(request.getFullName())
                .role(EnumRole.CUSTOMER)
                .build();

        userRepository.save(user);

        return  AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .password("********")
                .build();
    }

    @Override
    public LoginResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword()));

        User user = userRepository.findByUsernameAndIsDeletedFalse(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        if (EnumStatus.INACTIVE.equals(user.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }
        if (EnumStatus.DELETED.equals(user.getStatus())){
            throw new AppException(ErrorCode.USER_DELETED);
        }
        String token = jwtService.generateToken(user.getUsername());
        return LoginResponse.builder()
                .token(token)
                .build();
    }

    @Override
    public AuthResponse getUserByUsername(String username) {
        User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        return AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .password(user.getPassword())
                .email(user.getEmail())
                .build();
    }


}
