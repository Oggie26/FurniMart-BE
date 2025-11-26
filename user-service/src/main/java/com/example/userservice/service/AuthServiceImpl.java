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
import com.example.userservice.event.EmailVerificationEvent;
import com.example.userservice.event.OtpEvent;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.EmployeeRepository;
import com.example.userservice.repository.EmployeeStoreRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.ForgotPasswordRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.request.ResetPasswordRequest;
import com.example.userservice.request.VerifyEmailRequest;
import com.example.userservice.response.AuthResponse;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.AuthService;
import com.example.userservice.service.inteface.WalletService;
import jakarta.persistence.LockModeType;
import jakarta.transaction.Transactional;
import org.springframework.data.jpa.repository.Lock;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

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
    private final KafkaTemplate<String, EmailVerificationEvent> emailVerificationKafkaTemplate;
    private final KafkaTemplate<String, OtpEvent> otpKafkaTemplate;
    private final WalletService walletService;
    private final VerificationService verificationService;
    private final RateLimitService rateLimitService;
    
    // OTP brute force protection constants
    private static final int MAX_OTP_ATTEMPTS = 5;
    private static final int OTP_LOCKOUT_MINUTES = 15;
    
    // Kafka retry configuration
    private static final int KAFKA_RETRY_ATTEMPTS = 3;
    private static final long KAFKA_RETRY_DELAY_MS = 1000;

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
        
        // Generate verification token
        String verificationToken = verificationService.generateVerificationToken();
        LocalDateTime verificationTokenExpiry = verificationService.generateVerificationTokenExpiry();
        
        Account account = Account.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .status(EnumStatus.INACTIVE) // Set to INACTIVE until email is verified
                .role(EnumRole.CUSTOMER)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(verificationTokenExpiry)
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

        // Reload account to ensure we have the latest state from database
        Account savedAccount = accountRepository.findById(account.getId())
                .orElseThrow(() -> new AppException(ErrorCode.ACCOUNT_NOT_FOUND));

        // Auto-create wallet for new customer (in separate transaction to avoid rollback)
        // Flush to ensure user is persisted before creating wallet
        try {
            // Flush to ensure user and account are persisted to database
            userRepository.flush();
            accountRepository.flush(); // Also flush account to ensure it's visible
            log.debug("User and account flushed to database: {}", savedUser.getId());
            
            // Use separate transaction (REQUIRES_NEW) to avoid affecting main transaction
            // The REQUIRES_NEW transaction should see the flushed user
            walletService.createWalletForUser(savedUser.getId());
            log.info("Wallet auto-created for new customer: {}", savedUser.getId());
        } catch (AppException e) {
            log.error("Failed to auto-create wallet for user {}: ErrorCode={}, Message={}", 
                    savedUser.getId(), e.getErrorCode(), e.getMessage(), e);
            // Don't fail registration if wallet creation fails - wallet can be created later
        } catch (Exception e) {
            log.error("Failed to auto-create wallet for user {}: {}", savedUser.getId(), e.getMessage(), e);
            // Don't fail registration if wallet creation fails - wallet can be created later
        }

        final String accountEmail = savedAccount.getEmail();
        final String accountId = savedAccount.getId();
        
        AccountCreatedEvent event = new AccountCreatedEvent(accountId, user.getFullName(), accountEmail, EnumRole.CUSTOMER);
        
        // Send verification email event
        EmailVerificationEvent verificationEvent = EmailVerificationEvent.builder()
                .id(accountId)
                .fullName(user.getFullName())
                .email(accountEmail)
                .verificationToken(verificationToken)
                .build();

        try {
            kafkaTemplate.send("account-created-topic", event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send account creation event: {}", ex.getMessage());
                    } else {
                        log.info("Successfully sent account creation event for: {}", accountEmail);
                    }
                });
            
            // Send verification email with retry mechanism
            sendKafkaEventWithRetry(
                emailVerificationKafkaTemplate,
                "email-verification-topic",
                verificationEvent,
                savedAccount.getEmail(),
                "verification email"
            );
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
        
        // Check if email is verified - must verify before login
        if (!account.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        
        // Check account status after email verification
        if (EnumStatus.INACTIVE.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_BLOCKED);
        }

        Map<String, Object> claims;
        String storeId = "";

        if (!account.getRole().equals(EnumRole.CUSTOMER)) {
            Employee employee = employeeRepository.findByAccountIdAndIsDeletedFalse(account.getId())
                    .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

            List<EmployeeStore> employeeStores = employeeStoreRepository.findByEmployeeIdAndIsDeletedFalse(employee.getId());
            storeId = employeeStores.isEmpty() ? "" : employeeStores.getFirst().getStoreId();

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

    @Override
    @Transactional
    public void forgotPassword(ForgotPasswordRequest request) {
        // Rate limiting check
        try {
            rateLimitService.checkForgotPasswordRateLimit(request.getEmail());
        } catch (AppException e) {
            // Return silently to prevent email enumeration even on rate limit
            log.warn("Rate limit exceeded for forgot password: {}", request.getEmail());
            return;
        }
        
        // Security: Don't reveal if email exists or not to prevent email enumeration
        Optional<Account> accountOpt = accountRepository.findByEmailAndIsDeletedFalse(request.getEmail());
        
        if (accountOpt.isEmpty()) {
            // Log but don't reveal email doesn't exist (security best practice)
            log.warn("Forgot password requested for non-existent email: {}", request.getEmail());
            return; // Return silently to prevent email enumeration
        }
        
        Account account = accountOpt.get();

        if (EnumStatus.DELETED.equals(account.getStatus())) {
            log.warn("Forgot password requested for deleted account: {}", request.getEmail());
            return; // Return silently
        }

        // Generate OTP and reset token
        String otpCode = verificationService.generateOtpCode();
        String resetToken = verificationService.generateResetToken();
        LocalDateTime otpExpiry = verificationService.generateOtpExpiry();
        LocalDateTime resetTokenExpiry = verificationService.generateResetTokenExpiry();

        // Save OTP and reset token to account
        // Reset OTP attempts when generating new OTP
        account.setOtpCode(otpCode);
        account.setOtpExpiry(otpExpiry);
        account.setResetToken(resetToken);
        account.setResetTokenExpiry(resetTokenExpiry);
        account.setOtpAttempts(0); // Reset attempts for new OTP
        account.setOtpLockoutUntil(null); // Clear lockout
        accountRepository.save(account);

        // Get user info for email
        User user = userRepository.findByEmailAndIsDeletedFalse(request.getEmail())
                .orElse(null);
        String fullName = user != null ? user.getFullName() : "User";

        // Send OTP and reset token via email
        OtpEvent event = OtpEvent.builder()
                .id(account.getId())
                .fullName(fullName)
                .email(account.getEmail())
                .otpCode(otpCode)
                .resetToken(resetToken)
                .build();

        // Send OTP email with retry mechanism
        sendKafkaEventWithRetry(
            otpKafkaTemplate,
            "forgot-password-otp-topic",
            event,
            account.getEmail(),
            "forgot password OTP"
        );
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new AppException(ErrorCode.RESET_TOKEN_INVALID);
        }
        
        if (request.getNewPassword() == null || request.getNewPassword().length() < 6) {
            throw new AppException(ErrorCode.INVALID_PASSWORD);
        }
        
        Account account = accountRepository.findByResetToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.RESET_TOKEN_INVALID));

        if (account.getResetTokenExpiry() == null || verificationService.isTokenExpired(account.getResetTokenExpiry())) {
            throw new AppException(ErrorCode.RESET_TOKEN_EXPIRED);
        }

        if (EnumStatus.DELETED.equals(account.getStatus())) {
            throw new AppException(ErrorCode.USER_DELETED);
        }

        // OTP brute force protection check
        if (account.getOtpLockoutUntil() != null && account.getOtpLockoutUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // If lockout period has passed, reset attempts
        if (account.getOtpLockoutUntil() != null && account.getOtpLockoutUntil().isBefore(LocalDateTime.now())) {
            account.setOtpAttempts(0);
            account.setOtpLockoutUntil(null);
        }

        // Update password
        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // Clear reset token and OTP
        account.setResetToken(null);
        account.setResetTokenExpiry(null);
        account.setOtpCode(null);
        account.setOtpExpiry(null);
        account.setOtpAttempts(0);
        account.setOtpLockoutUntil(null);
        accountRepository.save(account);

        log.info("Password reset successfully for account: {}", account.getEmail());
    }

    @Override
    @Transactional
    public void verifyEmail(VerifyEmailRequest request) {
        if (request.getToken() == null || request.getToken().trim().isEmpty()) {
            throw new AppException(ErrorCode.VERIFICATION_TOKEN_INVALID);
        }
        
        // Rate limiting check
        try {
            rateLimitService.checkVerifyEmailRateLimit(request.getToken());
        } catch (AppException e) {
            throw e; // Re-throw rate limit exceptions
        }
        
        Account account = accountRepository.findByVerificationToken(request.getToken())
                .orElseThrow(() -> new AppException(ErrorCode.VERIFICATION_TOKEN_INVALID));

        if (account.getVerificationTokenExpiry() == null || verificationService.isTokenExpired(account.getVerificationTokenExpiry())) {
            throw new AppException(ErrorCode.VERIFICATION_TOKEN_EXPIRED);
        }

        // Verify email
        account.setEmailVerified(true);
        account.setStatus(EnumStatus.ACTIVE);
        account.setVerificationToken(null);
        account.setVerificationTokenExpiry(null);
        accountRepository.save(account);

        log.info("Email verified successfully for account: {}", account.getEmail());
    }

    @Override
    @Transactional
    public void resendVerificationEmail(String email) {
        if (email == null || email.trim().isEmpty()) {
            throw new AppException(ErrorCode.INVALID_REQUEST);
        }
        
        // Rate limiting check
        try {
            rateLimitService.checkResendVerificationRateLimit(email);
        } catch (AppException e) {
            throw e; // Re-throw rate limit exceptions
        }
        
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        if (account.isEmailVerified()) {
            throw new AppException(ErrorCode.EMAIL_EXISTS); // Email already verified
        }

        // Generate new verification token
        String verificationToken = verificationService.generateVerificationToken();
        LocalDateTime verificationTokenExpiry = verificationService.generateVerificationTokenExpiry();

        account.setVerificationToken(verificationToken);
        account.setVerificationTokenExpiry(verificationTokenExpiry);
        accountRepository.save(account);

        // Get user info
        User user = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElse(null);
        String fullName = user != null ? user.getFullName() : "User";

        // Send verification email
        EmailVerificationEvent event = EmailVerificationEvent.builder()
                .id(account.getId())
                .fullName(fullName)
                .email(account.getEmail())
                .verificationToken(verificationToken)
                .build();

        // Send verification email with retry mechanism
        sendKafkaEventWithRetry(
            emailVerificationKafkaTemplate,
            "email-verification-topic",
            event,
            account.getEmail(),
            "verification email"
        );
    }

    @Override
    @Transactional
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    public void verifyOtpForPasswordReset(String email, String otpCode) {
        Account account = accountRepository.findByEmailAndIsDeletedFalse(email)
                .orElseThrow(() -> new AppException(ErrorCode.NOT_FOUND_USER));

        // Check OTP lockout
        if (account.getOtpLockoutUntil() != null && account.getOtpLockoutUntil().isAfter(LocalDateTime.now())) {
            throw new AppException(ErrorCode.OTP_LOCKED);
        }

        // If lockout period has passed, reset attempts
        if (account.getOtpLockoutUntil() != null && account.getOtpLockoutUntil().isBefore(LocalDateTime.now())) {
            account.setOtpAttempts(0);
            account.setOtpLockoutUntil(null);
        }

        // Check if OTP exists and not expired
        if (account.getOtpCode() == null || verificationService.isOtpExpired(account.getOtpExpiry())) {
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        // Verify OTP
        if (!verificationService.verifyOtp(account.getOtpCode(), otpCode)) {
            // Increment failed attempts
            int attempts = account.getOtpAttempts() != null ? account.getOtpAttempts() : 0;
            attempts++;
            account.setOtpAttempts(attempts);

            // Lock account if max attempts reached
            if (attempts >= MAX_OTP_ATTEMPTS) {
                account.setOtpLockoutUntil(LocalDateTime.now().plusMinutes(OTP_LOCKOUT_MINUTES));
                log.warn("OTP locked for account: {} after {} failed attempts", email, attempts);
                accountRepository.save(account);
                throw new AppException(ErrorCode.OTP_LOCKED);
            }

            accountRepository.save(account);
            throw new AppException(ErrorCode.OTP_INVALID);
        }

        // OTP verified successfully - reset attempts
        account.setOtpAttempts(0);
        account.setOtpLockoutUntil(null);
        accountRepository.save(account);

        log.info("OTP verified successfully for account: {}", account.getEmail());
    }
    
    /**
     * Send Kafka event with retry mechanism
     * Retries up to KAFKA_RETRY_ATTEMPTS times with exponential backoff
     */
    private <T> void sendKafkaEventWithRetry(
            KafkaTemplate<String, T> kafkaTemplate,
            String topic,
            T event,
            String email,
            String eventType) {
        
        int attempt = 0;
        boolean success = false;
        Exception lastException = null;
        
        while (attempt < KAFKA_RETRY_ATTEMPTS && !success) {
            try {
                attempt++;
                CompletableFuture<org.springframework.kafka.support.SendResult<String, T>> future = 
                    kafkaTemplate.send(topic, event);
                
                // Wait for completion with timeout
                future.get(5, TimeUnit.SECONDS);
                
                log.info("Successfully sent {} for: {} (attempt {})", eventType, email, attempt);
                success = true;
                
            } catch (java.util.concurrent.TimeoutException e) {
                lastException = e;
                log.warn("Timeout sending {} for: {} (attempt {}/{})", eventType, email, attempt, KAFKA_RETRY_ATTEMPTS);
                
                if (attempt < KAFKA_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(KAFKA_RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        break;
                    }
                }
                
            } catch (Exception e) {
                lastException = e;
                log.warn("Failed to send {} for: {} (attempt {}/{}) - {}", 
                        eventType, email, attempt, KAFKA_RETRY_ATTEMPTS, e.getMessage());
                
                if (attempt < KAFKA_RETRY_ATTEMPTS) {
                    try {
                        Thread.sleep(KAFKA_RETRY_DELAY_MS * attempt); // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        log.error("Retry sleep interrupted", ie);
                        break;
                    }
                }
            }
        }
        
        if (!success) {
            log.error("Failed to send {} for: {} after {} attempts. Last error: {}", 
                    eventType, email, KAFKA_RETRY_ATTEMPTS, 
                    lastException != null ? lastException.getMessage() : "Unknown");
            // Don't throw exception to prevent revealing email existence or breaking user flow
            // Consider: Save to database queue for later processing
        }
    }
}