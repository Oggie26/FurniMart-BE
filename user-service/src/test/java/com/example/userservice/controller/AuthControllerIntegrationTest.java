package com.example.userservice.controller;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.request.RegisterRequest;
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
    @DisplayName("Should register new user successfully (no email verification)")
    @Transactional
    void testRegister_ShouldSucceed() throws Exception {
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
        assertEquals(EnumStatus.ACTIVE, savedAccount.getStatus()); // Should be ACTIVE immediately
    }

    @Test
    @DisplayName("Should login successfully (no email verification required)")
    @Transactional
    void testLogin_ShouldSucceed() throws Exception {
        // When & Then
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"existing@test.com\",\"password\":\"password123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.data.token").exists())
                .andExpect(jsonPath("$.data.refreshToken").exists());
    }
}
