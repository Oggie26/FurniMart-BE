package com.example.userservice.service;

import com.example.userservice.config.JwtService;
import com.example.userservice.entity.Account;
import com.example.userservice.entity.Employee;
import com.example.userservice.entity.EmployeeStore;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.event.AccountCreatedEvent;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.EmployeeStoreRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import com.example.userservice.service.inteface.WalletService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
    private final EmployeeRepository employeeRepository;
    private final EmployeeStoreRepository employeeStoreRepository;
    private final TokenService tokenService;
    private final KafkaTemplate<String, AccountCreatedEvent> kafkaTemplate;
    private final WalletService walletService;

    @Override
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        log.info("=== REGISTER METHOD CALLED ===");
        log.info("Register request - Email: {}, FullName: {}, Phone: {}", 
            request.getEmail(), request.getFullName(), request.getPhone());
        
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
                .status(EnumStatus.ACTIVE) // Set to ACTIVE immediately (no email verification)
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
        User savedUser = userRepository.save(user);

        Account savedAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Check transaction status
        boolean isActiveTransaction = TransactionSynchronizationManager.isActualTransactionActive();
        boolean isNewTransaction = TransactionSynchronizationManager.isCurrentTransactionReadOnly() == false;
        String transactionName = TransactionSynchronizationManager.getCurrentTransactionName();
        log.info("=== TRANSACTION STATUS ===");
        log.info("Is Active: {}, Is New: {}, Transaction Name: {}", 
            isActiveTransaction, isNewTransaction, transactionName);
        
        log.info("=== WALLET CREATION CHECK START ===");
        log.info("User ID: {}, Account ID: {}, Account Role: {}", 
            savedUser.getId(), savedAccount.getId(), savedAccount.getRole());
        log.info("Account Role Enum: {}, CUSTOMER Enum: {}, Match: {}", 
            savedAccount.getRole(), EnumRole.CUSTOMER, savedAccount.getRole() == EnumRole.CUSTOMER);

        // Create wallet directly in the same transaction for CUSTOMER
        // API register only creates CUSTOMER role, so wallet must be created here
        log.info("Checking role for wallet creation. User ID: {}, Account Role: {}", savedUser.getId(), savedAccount.getRole());
        if (savedAccount.getRole() == EnumRole.CUSTOMER) {
            log.info("User {} has CUSTOMER role. Proceeding with wallet creation.", savedUser.getId());
            try {
                // Ensure account is loaded in savedUser for wallet creation
                // savedUser.getAccount() might be null due to lazy loading, so we set it explicitly
                savedUser.setAccount(savedAccount);
                log.info("Account set in savedUser. Calling createWalletDirectlyInTransaction for user: {}", savedUser.getId());
                
                // Create wallet directly in the same transaction (no lazy, no retry needed)
                walletService.createWalletDirectlyInTransaction(savedUser.getId(), savedUser);
                log.info("Wallet created directly in transaction for new CUSTOMER: {}", savedUser.getId());
            } catch (AppException e) {
                log.error("Failed to create wallet for CUSTOMER {} during registration: ErrorCode={}, Message={}. " +
                        "Registration will fail to ensure data consistency.", 
                        savedUser.getId(), e.getErrorCode(), e.getMessage());
                throw e; // Fail registration if wallet creation fails
            } catch (Exception e) {
                log.error("Failed to create wallet for CUSTOMER {} during registration: {}. " +
                        "Registration will fail to ensure data consistency.", 
                        savedUser.getId(), e.getMessage(), e);
                throw new AppException(ErrorCode.INTERNAL_SERVER_ERROR);
            }
        } else {
            log.warn("User {} registered with role {}. Wallet not created (only CUSTOMER gets wallet).", 
                    savedUser.getId(), savedAccount.getRole());
        }

        final String accountEmail = savedAccount.getEmail();
        final String accountId = savedAccount.getId();
        
        AccountCreatedEvent event = new AccountCreatedEvent(accountId, user.getFullName(), accountEmail, EnumRole.CUSTOMER);

        try {
            kafkaTemplate.send("account-created-topic", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send account creation event: {}", ex.getMessage());
                    } else {
                        log.info("Successfully sent account creation event for: {}", accountEmail);
                    }
                });
        } catch (Exception e) {
            log.error("Failed to send Kafka event for account: {}, error: {}", accountEmail, e.getMessage());
        }

        return AuthResponse.builder()
                .id(accountId)
                .email(accountEmail)
                .role(savedAccount.getRole())
                .fullName(user.getFullName())
                .gender(user.getGender())
                .status(savedAccount.getStatus())
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

        if (EnumStatus.DELETED.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_DELETED);
        }
        
        // Check account status
        if (EnumStatus.INACTIVE.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        Map<String, Object> claims;
        String storeId = "";

        if (!account.getRole().equals(EnumRole.CUSTOMER) && !account.getRole().equals(EnumRole.ADMIN)) {
            Employee employee = employeeRepository.findByAccountIdAndIsDeletedFalse(account.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

            List<EmployeeStore> employeeStores = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(employee.getId());
            storeId = employeeStores.isEmpty() ? "" : employeeStores.get(0).getStoreId();

            claims = Map.of(
                    "role", account.getRole(),
                    "accountId", account.getId(),
                    "storeId", storeId
            );
        } else {
            // Customer login
            claims = Map.of(
                    "role", account.getRole(),
                    "accountId", account.getId()
            );
        }

        String accessToken = jwtService.generateToken(claims, account.getEmail());
        String refreshToken = jwtService.generateRefreshToken(claims, account.getEmail());

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