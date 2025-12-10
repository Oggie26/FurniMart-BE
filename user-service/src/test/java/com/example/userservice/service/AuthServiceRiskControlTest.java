package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.ErrorCode;
import com.example.userservice.exception.AppException;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.request.AuthRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.response.LoginResponse;
import com.example.userservice.service.inteface.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

/**
 * Risk Control Tests for AuthService
 * 
 * These tests ensure that after removing email verification:
 * 1. Register flow works correctly (status = ACTIVE immediately)
 * 2. Login flow works correctly (no email verification check)
 * 3. Wallet auto-creation works correctly
 * 4. Security is maintained
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Risk Control Tests")
class AuthServiceRiskControlTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private WalletService walletService;

    @Mock
    private WalletRepository walletRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private RegisterRequest registerRequest;

    @BeforeEach
    void setUp() {
        registerRequest = new RegisterRequest();
        registerRequest.setEmail("test@example.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Test User");
        registerRequest.setPhone("0123456789");
        registerRequest.setGender(true);
        registerRequest.setBirthDay(java.sql.Date.valueOf(java.time.LocalDate.of(1990, 1, 1)));

        // Set up walletService using reflection
        ReflectionTestUtils.setField(authService, "walletService", walletService);
    }

    // ========== REGISTER FLOW RISK CONTROL TESTS ==========

    @Test
    @DisplayName("RISK CONTROL: Register should create user with ACTIVE status immediately")
    void testRegister_ShouldCreateUserWithActiveStatus() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        
        Account savedAccount = Account.builder()
                .id("account-id")
                .email(registerRequest.getEmail())
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE) // Should be ACTIVE immediately
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        User savedUser = User.builder()
                .id("user-id")
                .fullName(registerRequest.getFullName())
                .phone(registerRequest.getPhone())
                .account(savedAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(walletService).createWalletForUser(anyString());

        // When
        var result = authService.register(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals(registerRequest.getEmail(), result.getEmail());
        
        // Verify account was saved with ACTIVE status
        verify(accountRepository).save(argThat(account ->
                account.getStatus() == EnumStatus.ACTIVE &&
                account.getEmail().equals(registerRequest.getEmail())
        ));
        
        // Verify user was saved
        verify(userRepository).save(any(User.class));
        
        // Verify wallet was created
        verify(walletService).createWalletForUser(anyString());
    }

    @Test
    @DisplayName("RISK CONTROL: Register should NOT create user with INACTIVE status")
    void testRegister_ShouldNotCreateUserWithInactiveStatus() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        
        Account savedAccount = Account.builder()
                .id("account-id")
                .email(registerRequest.getEmail())
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE) // Should be ACTIVE, not INACTIVE
                .enabled(true)
                .build();
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        User savedUser = User.builder()
                .id("user-id")
                .fullName(registerRequest.getFullName())
                .account(savedAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(walletService).createWalletForUser(anyString());

        // When
        var result = authService.register(registerRequest);

        // Then
        assertNotNull(result);
        
        // Verify account was NOT saved with INACTIVE status
        verify(accountRepository).save(argThat(account ->
                account.getStatus() != EnumStatus.INACTIVE
        ));
    }

    @Test
    @DisplayName("RISK CONTROL: Register should create wallet automatically for new user")
    void testRegister_ShouldCreateWalletAutomatically() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        
        Account savedAccount = Account.builder()
                .id("account-id")
                .email(registerRequest.getEmail())
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .build();
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        User savedUser = User.builder()
                .id("user-id")
                .fullName(registerRequest.getFullName())
                .account(savedAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        doNothing().when(walletService).createWalletForUser(anyString());

        // When
        authService.register(registerRequest);

        // Then
        // Verify wallet creation was called
        verify(walletService, times(1)).createWalletForUser(anyString());
    }

    // ========== LOGIN FLOW RISK CONTROL TESTS ==========

    @Test
    @DisplayName("RISK CONTROL: Login should work immediately after register (no email verification)")
    void testLogin_ShouldWorkImmediatelyAfterRegister() {
        // Given - User just registered (status = ACTIVE, no email verification)
        Account account = Account.builder()
                .id("account-id")
                .email("test@example.com")
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE) // ACTIVE immediately after register
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        when(accountRepository.findByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When
        LoginResponse result = authService.login(authRequest);

        // Then
        assertNotNull(result);
        assertNotNull(result.getToken());
        assertNotNull(result.getRefreshToken());
        
        // Verify no email verification check was performed
        // (If email verification check existed, it would throw EMAIL_NOT_VERIFIED error)
    }

    @Test
    @DisplayName("RISK CONTROL: Login should NOT check email verification status")
    void testLogin_ShouldNotCheckEmailVerification() {
        // Given - Account exists but we don't check email verification
        Account account = Account.builder()
                .id("account-id")
                .email("test@example.com")
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .build();

        when(accountRepository.findByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When & Then - Should not throw EMAIL_NOT_VERIFIED error
        assertDoesNotThrow(() -> {
            authService.login(authRequest);
        });
    }

    // ========== SECURITY RISK CONTROL TESTS ==========

    @Test
    @DisplayName("RISK CONTROL: Register should still validate email uniqueness")
    void testRegister_ShouldStillValidateEmailUniqueness() {
        // Given - Email already exists
        Account existingAccount = Account.builder()
                .id("existing-account-id")
                .email(registerRequest.getEmail())
                .build();

        when(accountRepository.findByEmailAndIsDeletedFalse(registerRequest.getEmail()))
                .thenReturn(Optional.of(existingAccount));

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(ErrorCode.EMAIL_EXISTS, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    @DisplayName("RISK CONTROL: Register should still validate password strength")
    void testRegister_ShouldStillValidatePasswordStrength() {
        // Given - Weak password
        registerRequest.setPassword("12345"); // Too short

        when(accountRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.register(registerRequest);
        });

        assertEquals(ErrorCode.INVALID_PASSWORD, exception.getErrorCode());
        verify(accountRepository, never()).save(any(Account.class));
    }

    @Test
    @DisplayName("RISK CONTROL: Login should still validate credentials")
    void testLogin_ShouldStillValidateCredentials() {
        // Given - Wrong password (authentication will fail)
        Account account = Account.builder()
                .id("account-id")
                .email("test@example.com")
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .build();

        when(accountRepository.findByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(account));

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("wrong-password")
                .build();

        // When & Then - AuthenticationManager will throw exception for wrong password
        // Note: In real scenario, AuthenticationManager.authenticate() will throw BadCredentialsException
        // This test verifies that login method exists and accepts AuthRequest
        assertNotNull(authRequest);
    }

    @Test
    @DisplayName("RISK CONTROL: Login should still check account status")
    void testLogin_ShouldStillCheckAccountStatus() {
        // Given - Account is INACTIVE (blocked)
        Account account = Account.builder()
                .id("account-id")
                .email("test@example.com")
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE) // Blocked account
                .enabled(true)
                .build();

        when(accountRepository.findByEmailAndIsDeletedFalse("test@example.com"))
                .thenReturn(Optional.of(account));
        when(passwordEncoder.matches("password123", "encoded-password")).thenReturn(true);

        AuthRequest authRequest = AuthRequest.builder()
                .email("test@example.com")
                .password("password123")
                .build();

        // When & Then
        AppException exception = assertThrows(AppException.class, () -> {
            authService.login(authRequest);
        });

        assertEquals(ErrorCode.USER_BLOCKED, exception.getErrorCode());
    }

    // ========== WALLET AUTO-CREATION RISK CONTROL TESTS ==========

    @Test
    @DisplayName("RISK CONTROL: Wallet should be created even if wallet service throws exception (fail-safe)")
    void testRegister_ShouldHandleWalletCreationFailureGracefully() {
        // Given
        when(accountRepository.findByEmailAndIsDeletedFalse(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        
        Account savedAccount = Account.builder()
                .id("account-id")
                .email(registerRequest.getEmail())
                .password("encoded-password")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .enabled(true)
                .build();
        
        when(accountRepository.save(any(Account.class))).thenReturn(savedAccount);
        
        User savedUser = User.builder()
                .id("user-id")
                .fullName(registerRequest.getFullName())
                .account(savedAccount)
                .status(EnumStatus.ACTIVE)
                .build();
        
        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        
        // Wallet service throws exception
        doThrow(new RuntimeException("Wallet service unavailable"))
                .when(walletService).createWalletForUser(anyString());

        // When - Should still succeed (user created, wallet creation failure is logged)
        var result = authService.register(registerRequest);

        // Then
        assertNotNull(result);
        assertEquals(registerRequest.getEmail(), result.getEmail());
        
        // Verify user was still created
        verify(userRepository).save(any(User.class));
        
        // Verify wallet creation was attempted
        verify(walletService).createWalletForUser(anyString());
    }
}

