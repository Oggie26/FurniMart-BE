package com.example.userservice.service;

import com.example.userservice.config.JwtService;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.event.AccountCreatedEvent;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserRepository userRepository;
    private final AccountRepository accountRepository;
    private final AuthenticationManager authenticationManager;
    private final TokenService tokenService;
    private final KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (request.getEmail() == null || request.getPassword() == null) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        if (request.getPassword().length() < 6) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        if(accountRepository.findByEmailAndIsDeletedFalse(request.getEmail()).isPresent()){
            throw new AppException(ErrorCode.EMAIL_EXISTS);
        }
        Account account = Account.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(EnumStatus.ACTIVE)
                .role(EnumRole.CUSTOMER)
                .build();

        User user = User.builder()
                .phone(request.getPhone())
                .fullName(request.getFullName())
                .status(EnumStatus.ACTIVE)
                .gender(request.getGender())
                .birthday(request.getBirthDay())
                .account(account)
                .build();

        accountRepository.save(account);
        userRepository.save(user);

        AccountCreatedEvent event = new AccountCreatedEvent(account.getId(),user.getFullName() , account.getEmail(),EnumRole.CUSTOMER);

        try {
            kafkaTemplate.send("account-created-topic", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                    } else {
                        log.info("Successfully sent account creation event for: {}", account.getEmail());
                    }
                });
        } catch (Exception e) {
            log.error("Failed to send Kafka event for account: {}, error: {}", account.getEmail(), e.getMessage());
        }

        return AuthResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .role(account.getRole())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .status(account.getStatus())
                .password("********")
                .build();
    }

    @Override
    public LoginResponse login(AuthRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        Account account = accountRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        if (EnumStatus.INACTIVE.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }
        if (EnumStatus.DELETED.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        List<String> storeIds = List.of();

        Map<String, Object> claims = Map.of(
                "role", account.getRole(),
                "accountId", account.getId(),
                "storeId", storeIds
        );
        String accessToken = jwtService.generateToken(claims, account.getEmail());
        String refreshToken = jwtService.generateRefreshToken(claims,account.getEmail());

        tokenService.saveToken(account.getEmail(), accessToken, jwtService.getJwtExpiration());
        tokenService.saveRefreshToken(account.getEmail(), refreshToken, jwtService.getRefreshExpiration());
        return LoginResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    @Override
    public AuthResponse getUserByUsername(String email) {
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));
        return AuthResponse.builder()
                .id(account.getId())
                .email(account.getEmail())
                .password(account.getPassword())
                .role(account.getRole())
                .status(account.getStatus())
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
            String email = jwtService.extractUsername(refreshToken);
            Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

            String storedRefreshToken = tokenService.getRefreshToken(email);
            if (!refreshToken.equals(storedRefreshToken)) {
                throw new AppException(ErrorCode.INVALID_TOKEN);
            }

            Map<String, Object> claims = Map.of("role", account.getRole());
            String newAccessToken = jwtService.generateToken(claims, account.getEmail());
            String newRefreshToken = jwtService.generateRefreshToken(claims,account.getEmail());

            tokenService.saveToken(email, newAccessToken, jwtService.getJwtExpiration());
            tokenService.saveRefreshToken(email, newRefreshToken, jwtService.getRefreshExpiration());

            return LoginResponse.builder()
                    .token(newAccessToken)
                    .refreshToken(newRefreshToken)
                    .build();

        } catch (Exception e) {
            throw new AppException(ErrorCode.INVALID_TOKEN);
        }
    }
}