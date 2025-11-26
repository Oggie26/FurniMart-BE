package com.example.userservice.integration;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.entity.Wallet;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.WalletStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.request.RegisterRequest;
import com.example.userservice.service.AuthServiceImpl;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration Tests for Risk Control
 * 
 * These tests verify the complete flow after removing email verification:
 * 1. Register → User ACTIVE → Wallet created → Can login immediately
 * 2. No email verification step required
 * 3. Security validations still work
 */
@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false",
    "spring.data.redis.repositories.enabled=false"
})
@ActiveProfiles("test")
@AutoConfigureMockMvc
@DisplayName("Auth Flow Risk Control Integration Tests")
class AuthFlowRiskControlIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private WalletRepository walletRepository;

    // Removed unused dependencies

    @BeforeEach
    @Transactional
    void setUp() {
        // Clean up test data
        walletRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    @DisplayName("RISK CONTROL: Complete flow - Register → ACTIVE → Wallet → Login")
    @Transactional
    void testCompleteFlow_RegisterToLogin() throws Exception {
        // Step 1: Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("newuser@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("New User");
        registerRequest.setPhone("0987654321");
        registerRequest.setGender(true);
        registerRequest.setBirthDay(java.sql.Date.valueOf(java.time.LocalDate.of(1990, 1, 1)));

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value(201))
                .andExpect(jsonPath("$.data.email").value("newuser@test.com"));

        // Step 2: Verify account is ACTIVE
        Account account = accountRepository.findByEmailAndIsDeletedFalse("newuser@test.com")
                .orElseThrow();
        assertEquals(EnumStatus.ACTIVE, account.getStatus(), 
                "Account should be ACTIVE immediately after register");

        // Step 3: Verify wallet was created
        User user = userRepository.findByEmailAndIsDeletedFalse("newuser@test.com")
                .orElseThrow();
        Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElse(null);
        assertNotNull(wallet, "Wallet should be created automatically");
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus(), 
                "Wallet should be ACTIVE");

        // Step 4: Login immediately (no email verification required)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"newuser@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }

    @Test
    @DisplayName("RISK CONTROL: User can login immediately after register")
    @Transactional
    void testUserCanLoginImmediatelyAfterRegister() throws Exception {
        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("immediate@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Immediate User");
        registerRequest.setPhone("0987654321");
        registerRequest.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Immediately try to login (no email verification step)
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"immediate@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").exists());
    }

    @Test
    @DisplayName("RISK CONTROL: Wallet is created with correct initial balance")
    @Transactional
    void testWalletCreatedWithCorrectInitialBalance() throws Exception {
        // Register
        RegisterRequest registerRequest = new RegisterRequest();
        registerRequest.setEmail("wallet@test.com");
        registerRequest.setPassword("password123");
        registerRequest.setFullName("Wallet User");
        registerRequest.setPhone("0987654321");
        registerRequest.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerRequest)))
                .andExpect(status().isCreated());

        // Verify wallet
        User user = userRepository.findByEmailAndIsDeletedFalse("wallet@test.com")
                .orElseThrow();
        Wallet wallet = walletRepository.findByUserIdAndIsDeletedFalse(user.getId())
                .orElseThrow();

        assertNotNull(wallet);
        assertEquals(0.0, wallet.getBalance().doubleValue(), 
                "Wallet should have initial balance of 0");
        assertEquals(WalletStatus.ACTIVE, wallet.getStatus());
        assertNotNull(wallet.getCode(), 
                "Wallet should have a unique code");
    }

    @Test
    @DisplayName("RISK CONTROL: Multiple users can register and login without email verification")
    @Transactional
    void testMultipleUsersCanRegisterAndLogin() throws Exception {
        // Register user 1
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("user1@test.com");
        request1.setPassword("password123");
        request1.setFullName("User 1");
        request1.setPhone("0987654321");
        request1.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Register user 2
        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("user2@test.com");
        request2.setPassword("password123");
        request2.setFullName("User 2");
        request2.setPhone("0987654322");
        request2.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isCreated());

        // Both users can login immediately
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user1@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user2@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk());

        // Verify both have wallets
        User user1 = userRepository.findByEmailAndIsDeletedFalse("user1@test.com")
                .orElseThrow();
        User user2 = userRepository.findByEmailAndIsDeletedFalse("user2@test.com")
                .orElseThrow();

        assertNotNull(walletRepository.findByUserIdAndIsDeletedFalse(user1.getId()));
        assertNotNull(walletRepository.findByUserIdAndIsDeletedFalse(user2.getId()));
    }

    @Test
    @DisplayName("RISK CONTROL: Security - Email uniqueness still enforced")
    @Transactional
    void testSecurity_EmailUniquenessEnforced() throws Exception {
        // Register first user
        RegisterRequest request1 = new RegisterRequest();
        request1.setEmail("unique@test.com");
        request1.setPassword("password123");
        request1.setFullName("First User");
        request1.setPhone("0987654321");
        request1.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        // Try to register with same email
        RegisterRequest request2 = new RegisterRequest();
        request2.setEmail("unique@test.com");
        request2.setPassword("password123");
        request2.setFullName("Second User");
        request2.setPhone("0987654322");
        request2.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(1205)) // EMAIL_EXISTS
                .andExpect(jsonPath("$.message").value("Email already exists"));
    }

    @Test
    @DisplayName("RISK CONTROL: Security - Password strength still validated")
    @Transactional
    void testSecurity_PasswordStrengthValidated() throws Exception {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("weakpass@test.com");
        request.setPassword("12345"); // Too short
        request.setFullName("Weak Pass User");
        request.setPhone("0987654321");
        request.setGender(true);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").exists())
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("Password")));
    }
}

