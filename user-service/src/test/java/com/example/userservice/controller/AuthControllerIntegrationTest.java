package com.example.userservice.controller;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.ForgotPasswordRequest;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.request.ResetPasswordRequest;
import com.example.userservice.request.VerifyEmailRequest;
import com.example.userservice.request.VerifyOtpRequest;
import com.example.userservice.service.VerificationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Auth Controller Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private VerificationService verificationService;

    private Account testAccount;
    private User testUser;

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up test data
        accountRepository.deleteAll();
        userRepository.deleteAll();

        // Create test account for login tests
        testAccount = Account.builder()
                .email("existing@test.com")
                .password(passwordEncoder.encode("password123")) // Encode password properly
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();

        testAccount = accountRepository.save(testAccount);

        testUser = User.builder()
                .fullName("Test User")
                .phone("0123456789")
                .status(EnumStatus.ACTIVE)
                .account(testAccount)
                .build();

        userRepository.save(testUser);
    }

    @Test
    @DisplayName("Should register new user and generate verification token")
    @Transactional
    void testRegister_ShouldGenerateVerificationToken() throws Exception {
        // Given
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@test.com");
        request.setPassword("password123");
        request.setFullName("New User");
        request.setPhone("0987654321");
        request.setGender(true);
        request.setBirthDay(java.sql.Date.valueOf(java.time.LocalDate.of(1990, 1, 1)));

        // When
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.message").value("Đăng kí thành công"))
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));

        // Then
        Account savedAccount = accountRepository.findByEmailAndIsDeletedFalse("newuser@test.com")
                .orElseThrow();
        assertFalse(savedAccount.isEmailVerified());
        assertNotNull(savedAccount.getVerificationToken());
        assertNotNull(savedAccount.getVerificationTokenExpiry());
        assertEquals(EnumStatus.INACTIVE, savedAccount.getStatus());
    }

    @Test
    @DisplayName("Should fail login when email not verified")
    @Transactional
    void testLogin_ShouldFailWhenEmailNotVerified() throws Exception {
        // Given - Create unverified account
        Account unverifiedAccount = Account.builder()
                .email("unverified@test.com")
                .password(passwordEncoder.encode("password123")) // Encode password properly
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .enabled(true)
                .accountNonExpired(true)
                .accountNonLocked(true)
                .credentialsNonExpired(true)
                .build();
        accountRepository.save(unverifiedAccount);

        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"unverified@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1253)) // EMAIL_NOT_VERIFIED error code
                .andExpect(jsonPath("$.message").value("Email is not verified"));
    }

    @Test
    @DisplayName("Should verify email successfully")
    @Transactional
    void testVerifyEmail_ShouldSucceed() throws Exception {
        // Given - Create account with verification token
        String verificationToken = verificationService.generateVerificationToken();
        Account account = Account.builder()
                .email("verify@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .enabled(true)
                .build();
        account = accountRepository.save(account);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(verificationToken);

        // When
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200));

        // Then
        Account verifiedAccount = accountRepository.findById(account.getId()).orElseThrow();
        assertTrue(verifiedAccount.isEmailVerified());
        assertEquals(EnumStatus.ACTIVE, verifiedAccount.getStatus());
        assertNull(verifiedAccount.getVerificationToken());
        assertNull(verifiedAccount.getVerificationTokenExpiry());
    }

    @Test
    @DisplayName("Should fail verify email with expired token")
    @Transactional
    void testVerifyEmail_ShouldFailWithExpiredToken() throws Exception {
        // Given
        String verificationToken = verificationService.generateVerificationToken();
        Account account = Account.builder()
                .email("expired@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().minusHours(1)) // Expired
                .enabled(true)
                .build();
        accountRepository.save(account);

        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken(verificationToken);

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1254)) // VERIFICATION_TOKEN_EXPIRED
                .andExpect(jsonPath("$.message").value("Verification token has expired"));
    }

    @Test
    @DisplayName("Should fail verify email with invalid token")
    @Transactional
    void testVerifyEmail_ShouldFailWithInvalidToken() throws Exception {
        // Given
        VerifyEmailRequest request = new VerifyEmailRequest();
        request.setToken("invalid-token-123");

        // When & Then
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1255)) // VERIFICATION_TOKEN_INVALID
                .andExpect(jsonPath("$.message").value("Invalid verification token"));
    }

    @Test
    @DisplayName("Should send forgot password OTP")
    @Transactional
    void testForgotPassword_ShouldSendOtp() throws Exception {
        // Given
        Account account = Account.builder()
                .email("forgot@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .enabled(true)
                .build();
        accountRepository.save(account);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("forgot@test.com");

        // When
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        Account updatedAccount = accountRepository.findByEmailAndIsDeletedFalse("forgot@test.com")
                .orElseThrow();
        assertNotNull(updatedAccount.getOtpCode());
        assertNotNull(updatedAccount.getOtpExpiry());
        assertNotNull(updatedAccount.getResetToken());
        assertNotNull(updatedAccount.getResetTokenExpiry());
        assertEquals(0, updatedAccount.getOtpAttempts());
    }

    @Test
    @DisplayName("Should not reveal email existence in forgot password")
    @Transactional
    void testForgotPassword_ShouldNotRevealEmailExistence() throws Exception {
        // Given - Non-existent email
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@test.com");

        // When & Then - Should return 200 (not 404)
        mockMvc.perform(post("/api/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk()); // Not 404 to prevent email enumeration
    }

    @Test
    @DisplayName("Should verify OTP successfully")
    @Transactional
    void testVerifyOtp_ShouldSucceed() throws Exception {
        // Given
        String otpCode = verificationService.generateOtpCode();
        Account account = Account.builder()
                .email("otp@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .otpCode(otpCode)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpAttempts(0)
                .enabled(true)
                .build();
        accountRepository.save(account);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpCode(otpCode);

        // When
        mockMvc.perform(post("/api/auth/verify-otp")
                        .param("email", "otp@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        Account updatedAccount = accountRepository.findByEmailAndIsDeletedFalse("otp@test.com")
                .orElseThrow();
        assertEquals(0, updatedAccount.getOtpAttempts());
        assertNull(updatedAccount.getOtpLockoutUntil());
    }

    @Test
    @DisplayName("Should fail verify OTP with wrong code")
    @Transactional
    void testVerifyOtp_ShouldFailWithWrongCode() throws Exception {
        // Given
        String correctOtp = verificationService.generateOtpCode();
        Account account = Account.builder()
                .email("wrongotp@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .otpCode(correctOtp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpAttempts(0)
                .enabled(true)
                .build();
        accountRepository.save(account);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpCode("000000"); // Wrong OTP

        // When
        mockMvc.perform(post("/api/auth/verify-otp")
                        .param("email", "wrongotp@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1259)) // OTP_INVALID
                .andExpect(jsonPath("$.message").value("Invalid OTP code"));

        // Then
        Account updatedAccount = accountRepository.findByEmailAndIsDeletedFalse("wrongotp@test.com")
                .orElseThrow();
        assertEquals(1, updatedAccount.getOtpAttempts());
    }

    @Test
    @DisplayName("Should lock OTP after 5 failed attempts")
    @Transactional
    void testVerifyOtp_ShouldLockAfterMaxAttempts() throws Exception {
        // Given
        String correctOtp = verificationService.generateOtpCode();
        Account account = Account.builder()
                .email("lockotp@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .otpCode(correctOtp)
                .otpExpiry(LocalDateTime.now().plusMinutes(10))
                .otpAttempts(4) // Already 4 attempts
                .enabled(true)
                .build();
        accountRepository.save(account);

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setOtpCode("000000"); // Wrong OTP - 5th attempt

        // When
        mockMvc.perform(post("/api/auth/verify-otp")
                        .param("email", "lockotp@test.com")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1261)) // OTP_LOCKED
                .andExpect(jsonPath("$.message").value("OTP verification locked due to too many failed attempts. Please try again later"));

        // Then
        Account lockedAccount = accountRepository.findByEmailAndIsDeletedFalse("lockotp@test.com")
                .orElseThrow();
        assertNotNull(lockedAccount.getOtpLockoutUntil());
        assertTrue(lockedAccount.getOtpLockoutUntil().isAfter(LocalDateTime.now()));
    }

    @Test
    @DisplayName("Should reset password successfully")
    @Transactional
    void testResetPassword_ShouldSucceed() throws Exception {
        // Given
        String resetToken = verificationService.generateResetToken();
        String oldPasswordHash = passwordEncoder.encode("oldpassword123");
        Account account = Account.builder()
                .email("reset@test.com")
                .password(oldPasswordHash)
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .resetToken(resetToken)
                .resetTokenExpiry(LocalDateTime.now().plusHours(1))
                .enabled(true)
                .build();
        accountRepository.save(account);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword("newpassword123");

        // When
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        Account updatedAccount = accountRepository.findByEmailAndIsDeletedFalse("reset@test.com")
                .orElseThrow();
        assertNotEquals(oldPasswordHash, updatedAccount.getPassword());
        assertTrue(passwordEncoder.matches("newpassword123", updatedAccount.getPassword()));
        assertNull(updatedAccount.getResetToken());
        assertNull(updatedAccount.getResetTokenExpiry());
        assertNull(updatedAccount.getOtpCode());
    }

    @Test
    @DisplayName("Should fail reset password with expired token")
    @Transactional
    void testResetPassword_ShouldFailWithExpiredToken() throws Exception {
        // Given
        String resetToken = verificationService.generateResetToken();
        Account account = Account.builder()
                .email("expiredreset@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .resetToken(resetToken)
                .resetTokenExpiry(LocalDateTime.now().minusHours(1)) // Expired
                .enabled(true)
                .build();
        accountRepository.save(account);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword("newpassword123");

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1256)) // RESET_TOKEN_EXPIRED
                .andExpect(jsonPath("$.message").value("Reset token has expired"));
    }

    @Test
    @DisplayName("Should fail reset password with invalid token")
    @Transactional
    void testResetPassword_ShouldFailWithInvalidToken() throws Exception {
        // Given
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken("invalid-token-123");
        request.setNewPassword("newpassword123");

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1257)) // RESET_TOKEN_INVALID
                .andExpect(jsonPath("$.message").value("Invalid reset token"));
    }

    @Test
    @DisplayName("Should fail reset password with weak password")
    @Transactional
    void testResetPassword_ShouldFailWithWeakPassword() throws Exception {
        // Given
        String resetToken = verificationService.generateResetToken();
        Account account = Account.builder()
                .email("weakpass@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .resetToken(resetToken)
                .resetTokenExpiry(LocalDateTime.now().plusHours(1))
                .enabled(true)
                .build();
        accountRepository.save(account);

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setToken(resetToken);
        request.setNewPassword("12345"); // Too short

        // When & Then
        mockMvc.perform(post("/api/auth/reset-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password")));
    }

    @Test
    @DisplayName("Should resend verification email")
    @Transactional
    void testResendVerificationEmail_ShouldSucceed() throws Exception {
        // Given
        String oldToken = verificationService.generateVerificationToken();
        Account account = Account.builder()
                .email("resend@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .verificationToken(oldToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .enabled(true)
                .build();
        accountRepository.save(account);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("resend@test.com");

        // When
        mockMvc.perform(post("/api/auth/resend-verification-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        // Then
        Account updatedAccount = accountRepository.findByEmailAndIsDeletedFalse("resend@test.com")
                .orElseThrow();
        assertNotNull(updatedAccount.getVerificationToken());
        assertNotEquals(oldToken, updatedAccount.getVerificationToken()); // New token generated
    }

    @Test
    @DisplayName("Should fail resend verification for already verified email")
    @Transactional
    void testResendVerificationEmail_ShouldFailForVerifiedEmail() throws Exception {
        // Given
        Account account = Account.builder()
                .email("verified@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .emailVerified(true)
                .enabled(true)
                .build();
        accountRepository.save(account);

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("verified@test.com");

        // When & Then
        mockMvc.perform(post("/api/auth/resend-verification-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1205)) // EMAIL_EXISTS
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("Should login successfully after email verification")
    @Transactional
    void testLogin_ShouldSucceedAfterVerification() throws Exception {
        // Given - Verify email first
        String verificationToken = verificationService.generateVerificationToken();
        Account account = Account.builder()
                .email("login@test.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.INACTIVE)
                .emailVerified(false)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(LocalDateTime.now().plusHours(24))
                .enabled(true)
                .build();
        account = accountRepository.save(account);

        // Verify email
        VerifyEmailRequest verifyRequest = new VerifyEmailRequest();
        verifyRequest.setToken(verificationToken);
        mockMvc.perform(post("/api/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(verifyRequest)))
                .andExpect(status().isOk());

        // When - Try to login
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"login@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }
}

