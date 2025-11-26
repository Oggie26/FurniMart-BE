package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.event.EmailVerificationEvent;
import com.example.userservice.event.OtpEvent;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ForgotPasswordRequest;
import com.example.userservice.request.ResetPasswordRequest;
import com.example.userservice.request.VerifyEmailRequest;
import com.example.userservice.service.RateLimitService;
import com.example.userservice.service.VerificationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceUnitTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private VerificationService verificationService;

    @Mock
    private RateLimitService rateLimitService;

    @Mock
    private KafkaTemplate<String, EmailVerificationEvent> emailVerificationKafkaTemplate;

    @Mock
    private KafkaTemplate<String, OtpEvent> otpKafkaTemplate;

    @InjectMocks
    private AuthServiceImpl authService;

    private Account testAccount;
    private User testUser;
    private String testEmail;
    private String testToken;
    private String testOtpCode;

    @BeforeEach
    void setUp() {
        testEmail = "test@example.com";
        testToken = "verification-token-123";
        testOtpCode = "123456";

        testAccount = Account.builder()
                .id("account-id-1")
                .email(testEmail)
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .verificationToken(testToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        testUser = User.builder()
                .id("user-id-1")
                .fullName("Test User")
                .account(testAccount)
                .status(EnumStatus.ACTIVE)
                .build();

        // Set up Kafka templates using reflection
        ReflectionTestUtils.setField(authService, "emailVerificationKafkaTemplate", emailVerificationKafkaTemplate);
        ReflectionTestUtils.setField(authService, "otpKafkaTemplate", otpKafkaTemplate);
    }

    // ========== verifyEmail Tests ==========

    @Test
    @DisplayName("Should verify email successfully with valid token")
    void testVerifyEmail_ShouldSucceedWithValidToken() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(testToken);

        doNothing().when(rateLimitService).checkVerifyEmailRateLimit(testToken);
        when(accountRepository.findByVerificationToken(testToken)).thenReturn(Optional.of(testAccount));
        when(verificationService.isTokenExpired(testAccount.getVerificationTokenExpiry())).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        assertDoesNotThrow(() -> authService.verifyEmail(request));

        // Then
        verify(rateLimitService).checkVerifyEmailRateLimit(testToken);
        verify(accountRepository).findByVerificationToken(testToken);
        verify(verificationService).isTokenExpired(any(LocalDateTime.class));
        verify(accountRepository).save(argThat(account ->
                account.isEmailVerified() &&
                account.getStatus() == EnumStatus.ACTIVE &&
                account.getVerificationToken() == null &&
                account.getVerificationTokenExpiry() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when token is null")
    void testVerifyEmail_ShouldThrowExceptionWhenTokenIsNull() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(null);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
        verify(accountRepository, never()).findByVerificationToken(anyString());
    }

    @Test
    @DisplayName("Should throw exception when token is empty")
    void testVerifyEmail_ShouldThrowExceptionWhenTokenIsEmpty() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("   ");

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void testVerifyEmail_ShouldThrowExceptionWhenTokenNotFound() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(testToken);

        doNothing().when(rateLimitService).checkVerifyEmailRateLimit(testToken);
        when(accountRepository.findByVerificationToken(testToken)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.VERIFICATION_TOKEN_INVALID, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when token expired")
    void testVerifyEmail_ShouldThrowExceptionWhenTokenExpired() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(testToken);

        doNothing().when(rateLimitService).checkVerifyEmailRateLimit(testToken);
        when(accountRepository.findByVerificationToken(testToken)).thenReturn(Optional.of(testAccount));
        when(verificationService.isTokenExpired(testAccount.getVerificationTokenExpiry())).thenReturn(true);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.VERIFICATION_TOKEN_EXPIRED, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("Should throw exception when rate limit exceeded")
    void testVerifyEmail_ShouldThrowExceptionWhenRateLimitExceeded() {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(testToken);

        doThrow(new AppException(ErrorCode.RATE_LIMIT_EXCEEDED))
                .when(rateLimitService).checkVerifyEmailRateLimit(testToken);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.verifyEmail(request));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(accountRepository, never()).findByVerificationToken(anyString());
    }

    // ========== resendVerificationEmail Tests ==========

    @Test
    @DisplayName("Should resend verification email successfully")
    void testResendVerificationEmail_ShouldSucceed() {
        // Given
        String newToken = "new-verification-token";
        LocalDateTime newExpiry = LocalDateTime.now().plusHours(24);

        doNothing().when(rateLimitService).checkResendVerificationRateLimit(testEmail);
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(userRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testUser));
        when(verificationService.generateVerificationToken()).thenReturn(newToken);
        when(verificationService.generateVerificationTokenExpiry()).thenReturn(newExpiry);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        @SuppressWarnings("unchecked")
        SendResult<String, EmailVerificationEvent> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, EmailVerificationEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(emailVerificationKafkaTemplate.send(anyString(), any(EmailVerificationEvent.class))).thenReturn(future);

        // When
        assertDoesNotThrow(() -> authService.resendVerificationEmail(testEmail));

        // Then
        verify(rateLimitService).checkResendVerificationRateLimit(testEmail);
        verify(accountRepository).findByEmailAndIsDeletedFalse(testEmail);
        verify(verificationService).generateVerificationToken();
        verify(verificationService).generateVerificationTokenExpiry();
        verify(accountRepository).save(argThat(account ->
                account.getVerificationToken().equals(newToken) &&
                account.getVerificationTokenExpiry().equals(newExpiry)
        ));
        verify(emailVerificationKafkaTemplate, atLeastOnce()).send(eq("email-verification-topic"), any(EmailVerificationEvent.class));
    }

    @Test
    @DisplayName("Should throw exception when email is null")
    void testResendVerificationEmail_ShouldThrowExceptionWhenEmailIsNull() {
        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resendVerificationEmail(null));
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(anyString());
    }

    @Test
    @DisplayName("Should throw exception when email is empty")
    void testResendVerificationEmail_ShouldThrowExceptionWhenEmailIsEmpty() {
        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resendVerificationEmail("   "));
        assertEquals(ErrorCode.INVALID_REQUEST, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void testResendVerificationEmail_ShouldThrowExceptionWhenAccountNotFound() {
        // Given
        doNothing().when(rateLimitService).checkResendVerificationRateLimit(testEmail);
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resendVerificationEmail(testEmail));
        assertEquals(ErrorCode.NOT_FOUND_USER, exception.getErrorCode());
        verify(verificationService, never()).generateVerificationToken();
    }

    @Test
    @DisplayName("Should throw exception when email already verified")
    void testResendVerificationEmail_ShouldThrowExceptionWhenEmailAlreadyVerified() {
        // Given
        testAccount.setEmailVerified(true);

        doNothing().when(rateLimitService).checkResendVerificationRateLimit(testEmail);
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resendVerificationEmail(testEmail));
        assertEquals(ErrorCode.EMAIL_EXISTS, exception.getErrorCode());
        verify(verificationService, never()).generateVerificationToken();
    }

    @Test
    @DisplayName("Should throw exception when rate limit exceeded")
    void testResendVerificationEmail_ShouldThrowExceptionWhenRateLimitExceeded() {
        // Given
        doThrow(new AppException(ErrorCode.RATE_LIMIT_EXCEEDED))
                .when(rateLimitService).checkResendVerificationRateLimit(testEmail);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resendVerificationEmail(testEmail));
        assertEquals(ErrorCode.RATE_LIMIT_EXCEEDED, exception.getErrorCode());
        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(anyString());
    }

    // ========== forgotPassword Tests ==========

    @Test
    @DisplayName("Should send OTP successfully for existing account")
    void testForgotPassword_ShouldSucceedForExistingAccount() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(testEmail);

        String otpCode = "123456";
        String resetToken = "reset-token-123";
        LocalDateTime otpExpiry = LocalDateTime.now().plusMinutes(10);
        LocalDateTime resetTokenExpiry = LocalDateTime.now().plusHours(1);

        doNothing().when(rateLimitService).checkForgotPasswordRateLimit(testEmail);
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(userRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testUser));
        when(verificationService.generateOtpCode()).thenReturn(otpCode);
        when(verificationService.generateResetToken()).thenReturn(resetToken);
        when(verificationService.generateOtpExpiry()).thenReturn(otpExpiry);
        when(verificationService.generateResetTokenExpiry()).thenReturn(resetTokenExpiry);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        @SuppressWarnings("unchecked")
        SendResult<String, OtpEvent> sendResult = mock(SendResult.class);
        CompletableFuture<SendResult<String, OtpEvent>> future = CompletableFuture.completedFuture(sendResult);
        when(otpKafkaTemplate.send(anyString(), any(OtpEvent.class))).thenReturn(future);

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then
        verify(rateLimitService).checkForgotPasswordRateLimit(testEmail);
        verify(accountRepository).findByEmailAndIsDeletedFalse(testEmail);
        verify(verificationService).generateOtpCode();
        verify(verificationService).generateResetToken();
        verify(accountRepository).save(argThat(account ->
                account.getOtpCode().equals(otpCode) &&
                account.getResetToken().equals(resetToken) &&
                account.getOtpAttempts() == 0 &&
                account.getOtpLockoutUntil() == null
        ));
        verify(otpKafkaTemplate, atLeastOnce()).send(eq("forgot-password-otp-topic"), any(OtpEvent.class));
    }

    @Test
    @DisplayName("Should return silently when email not found (security)")
    void testForgotPassword_ShouldReturnSilentlyWhenEmailNotFound() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@example.com");

        doNothing().when(rateLimitService).checkForgotPasswordRateLimit("nonexistent@example.com");
        when(accountRepository.findByEmailAndIsDeletedFalse("nonexistent@example.com")).thenReturn(Optional.empty());

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then - Should return silently (security best practice)
        verify(verificationService, never()).generateOtpCode();
        verify(otpKafkaTemplate, never()).send(anyString(), any());
    }

    @Test
    @DisplayName("Should return silently when rate limit exceeded")
    void testForgotPassword_ShouldReturnSilentlyWhenRateLimitExceeded() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(testEmail);

        doThrow(new AppException(ErrorCode.RATE_LIMIT_EXCEEDED))
                .when(rateLimitService).checkForgotPasswordRateLimit(testEmail);

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then - Should return silently (security best practice)
        verify(accountRepository, never()).findByEmailAndIsDeletedFalse(anyString());
    }

    @Test
    @DisplayName("Should return silently when account is deleted")
    void testForgotPassword_ShouldReturnSilentlyWhenAccountDeleted() {
        // Given
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail(testEmail);

        testAccount.setStatus(EnumStatus.DELETED);

        doNothing().when(rateLimitService).checkForgotPasswordRateLimit(testEmail);
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));

        // When
        assertDoesNotThrow(() -> authService.forgotPassword(request));

        // Then - Should return silently
        verify(verificationService, never()).generateOtpCode();
    }

    // ========== resetPassword Tests ==========

    @Test
    @DisplayName("Should reset password successfully with valid token")
    void testResetPassword_ShouldSucceedWithValidToken() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("newPassword123");

        testAccount.setResetToken("reset-token-123");
        testAccount.setResetTokenExpiry(LocalDateTime.now().plusHours(1));

        when(accountRepository.findByResetToken("reset-token-123")).thenReturn(Optional.of(testAccount));
        when(verificationService.isTokenExpired(testAccount.getResetTokenExpiry())).thenReturn(false);
        when(passwordEncoder.encode("newPassword123")).thenReturn("encoded-new-password");
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        assertDoesNotThrow(() -> authService.resetPassword(request));

        // Then
        verify(accountRepository).findByResetToken("reset-token-123");
        verify(verificationService).isTokenExpired(any(LocalDateTime.class));
        verify(passwordEncoder).encode("newPassword123");
        verify(accountRepository).save(argThat(account ->
                account.getPassword().equals("encoded-new-password") &&
                account.getResetToken() == null &&
                account.getResetTokenExpiry() == null &&
                account.getOtpCode() == null &&
                account.getOtpExpiry() == null &&
                account.getOtpAttempts() == 0 &&
                account.getOtpLockoutUntil() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when token is null")
    void testResetPassword_ShouldThrowExceptionWhenTokenIsNull() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(null);
        request.setNewPassword("newPassword123");

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.RESET_TOKEN_INVALID, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when token not found")
    void testResetPassword_ShouldThrowExceptionWhenTokenNotFound() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token");
        request.setNewPassword("newPassword123");

        when(accountRepository.findByResetToken("invalid-token")).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.RESET_TOKEN_INVALID, exception.getErrorCode());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when token expired")
    void testResetPassword_ShouldThrowExceptionWhenTokenExpired() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("newPassword123");

        testAccount.setResetToken("reset-token-123");
        testAccount.setResetTokenExpiry(LocalDateTime.now().minusHours(1));

        when(accountRepository.findByResetToken("reset-token-123")).thenReturn(Optional.of(testAccount));
        when(verificationService.isTokenExpired(testAccount.getResetTokenExpiry())).thenReturn(true);

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.RESET_TOKEN_EXPIRED, exception.getErrorCode());
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    @DisplayName("Should throw exception when password is too short")
    void testResetPassword_ShouldThrowExceptionWhenPasswordTooShort() {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("reset-token-123");
        request.setNewPassword("12345"); // Too short

        // When & Then - Password validation happens first, before checking token
        AppException exception = assertThrows(AppException.class, () -> authService.resetPassword(request));
        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
        verify(accountRepository, never()).findByResetToken(anyString());
        verify(passwordEncoder, never()).encode(anyString());
    }

    // ========== verifyOtpForPasswordReset Tests ==========

    @Test
    @DisplayName("Should verify OTP successfully")
    void testVerifyOtpForPasswordReset_ShouldSucceed() {
        // Given
        testAccount.setOtpCode(testOtpCode);
        testAccount.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        testAccount.setOtpAttempts(0);
        testAccount.setOtpLockoutUntil(null);

        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(verificationService.isOtpExpired(testAccount.getOtpExpiry())).thenReturn(false);
        when(verificationService.verifyOtp(testAccount.getOtpCode(), testOtpCode)).thenReturn(true);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When
        assertDoesNotThrow(() -> authService.verifyOtpForPasswordReset(testEmail, testOtpCode));

        // Then
        verify(accountRepository).findByEmailAndIsDeletedFalse(testEmail);
        verify(verificationService).isOtpExpired(testAccount.getOtpExpiry());
        verify(accountRepository).save(argThat(account ->
                account.getOtpAttempts() == 0 &&
                account.getOtpLockoutUntil() == null
        ));
    }

    @Test
    @DisplayName("Should throw exception when account not found")
    void testVerifyOtpForPasswordReset_ShouldThrowExceptionWhenAccountNotFound() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> authService.verifyOtpForPasswordReset(testEmail, testOtpCode));
        assertEquals(ErrorCode.NOT_FOUND_USER, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when OTP expired")
    void testVerifyOtpForPasswordReset_ShouldThrowExceptionWhenOtpExpired() {
        // Given
        testAccount.setOtpCode(testOtpCode);
        testAccount.setOtpExpiry(LocalDateTime.now().minusMinutes(1));

        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(verificationService.isOtpExpired(testAccount.getOtpExpiry())).thenReturn(true);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> authService.verifyOtpForPasswordReset(testEmail, testOtpCode));
        assertEquals(ErrorCode.OTP_EXPIRED, exception.getErrorCode());
    }

    @Test
    @DisplayName("Should throw exception when OTP is locked")
    void testVerifyOtpForPasswordReset_ShouldThrowExceptionWhenOtpLocked() {
        // Given
        testAccount.setOtpCode(testOtpCode);
        testAccount.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        testAccount.setOtpLockoutUntil(LocalDateTime.now().plusMinutes(5)); // Locked until future

        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        // OTP lockout check happens before OTP expiry check, so no need to mock isOtpExpired

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> authService.verifyOtpForPasswordReset(testEmail, testOtpCode));
        assertEquals(ErrorCode.OTP_LOCKED, exception.getErrorCode());
        verify(accountRepository).findByEmailAndIsDeletedFalse(testEmail);
        verify(verificationService, never()).isOtpExpired(any());
        verify(verificationService, never()).verifyOtp(anyString(), anyString());
    }

    @Test
    @DisplayName("Should increment OTP attempts on wrong OTP")
    void testVerifyOtpForPasswordReset_ShouldIncrementAttemptsOnWrongOtp() {
        // Given
        testAccount.setOtpCode("123456");
        testAccount.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        testAccount.setOtpAttempts(1);
        testAccount.setOtpLockoutUntil(null);

        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(verificationService.isOtpExpired(testAccount.getOtpExpiry())).thenReturn(false);
        when(verificationService.verifyOtp(testAccount.getOtpCode(), "wrong-otp")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> authService.verifyOtpForPasswordReset(testEmail, "wrong-otp"));
        assertEquals(ErrorCode.OTP_INVALID, exception.getErrorCode());
        
        verify(accountRepository).save(argThat(account ->
                account.getOtpAttempts() == 2
        ));
    }

    @Test
    @DisplayName("Should lock OTP after 5 failed attempts (MAX_OTP_ATTEMPTS)")
    void testVerifyOtpForPasswordReset_ShouldLockOtpAfterMaxFailedAttempts() {
        // Given - MAX_OTP_ATTEMPTS = 5, so set attempts to 4
        testAccount.setOtpCode("123456");
        testAccount.setOtpExpiry(LocalDateTime.now().plusMinutes(10));
        testAccount.setOtpAttempts(4); // Already 4 failed attempts, next will be 5 (lock)
        testAccount.setOtpLockoutUntil(null);

        when(accountRepository.findByEmailAndIsDeletedFalse(testEmail)).thenReturn(Optional.of(testAccount));
        when(verificationService.isOtpExpired(testAccount.getOtpExpiry())).thenReturn(false);
        when(verificationService.verifyOtp(testAccount.getOtpCode(), "wrong-otp")).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);

        // When & Then
        AppException exception = assertThrows(AppException.class, 
                () -> authService.verifyOtpForPasswordReset(testEmail, "wrong-otp"));
        assertEquals(ErrorCode.OTP_LOCKED, exception.getErrorCode()); // Should be OTP_LOCKED after 5 attempts
        
        verify(accountRepository, atLeastOnce()).save(argThat(account ->
                account.getOtpAttempts() == 5 &&
                account.getOtpLockoutUntil() != null
        ));
    }
}

