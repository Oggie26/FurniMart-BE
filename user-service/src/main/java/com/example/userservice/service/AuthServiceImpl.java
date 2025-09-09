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

import java.util.Date;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;

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

        // Check username exists
        if (userRepository.findByUsernameAndIsDeletedFalse(request.getUsername()).isPresent()) {
            throw new AppException(ErrorCode.USER_ALREADY_EXISTS);
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

        return AuthResponse.builder()
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
                new UsernamePasswordAuthenticationToken(request.getUsername(), request.getPassword())
        );

        User user = userRepository.findByUsernameAndIsDeletedFalse(request.getUsername())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        if (EnumStatus.INACTIVE.equals(user.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }
        if (EnumStatus.DELETED.equals(user.getStatus())) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        Map<String, Object> claims = Map.of("role", user.getRole().name());

        String accessToken = jwtService.generateToken(claims, user.getUsername());
        String refreshToken = jwtService.generateRefreshToken(user.getUsername());

        tokenService.saveToken(user.getUsername(), accessToken, jwtService.getJwtExpiration());
        tokenService.saveRefreshToken(user.getUsername(), refreshToken, jwtService.getRefreshExpiration());

        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
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
                .fullName(user.getFullName())
                .gender(user.getGender())
                .role(user.getRole())
                .status(user.getStatus())
                .build();
    }

    @Override
    public void logout(String token) {
        try {
            Date expiration = jwtService.extractExpiration(token);
            long remainingTime = expiration.getTime() - System.currentTimeMillis();

            if (remainingTime > 0) {
                tokenService.blacklistToken(token, remainingTime);
                log.info("Token {} added to blacklist with remaining time: {} ms", token, remainingTime);
            }

            String username = jwtService.extractUsername(token);
            tokenService.deleteToken(username);

        } catch (Exception e) {
            log.error("Error during logout: {}", e.getMessage());
            tokenService.blacklistToken(token, 3600000);
        }
    }

    @Override
    public LoginResponse refreshToken(String refreshToken) {
        try {
            String username = jwtService.extractUsername(refreshToken);
            User user = userRepository.findByUsernameAndIsDeletedFalse(username)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

            String storedRefreshToken = tokenService.getRefreshToken(username);
            if (!refreshToken.equals(storedRefreshToken)) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            Map<String, Object> claims = Map.of("role", user.getRole().name());
            String newAccessToken = jwtService.generateToken(claims, user.getUsername());
            String newRefreshToken = jwtService.generateRefreshToken(user.getUsername());

            tokenService.saveToken(username, newAccessToken, jwtService.getJwtExpiration());
            tokenService.saveRefreshToken(username, newRefreshToken, jwtService.getRefreshExpiration());

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}