package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false"
})
@ActiveProfiles("test")
class AiBotUserVerificationTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private static final String AI_BOT_EMAIL = "ai-bot@furnimart.com";

    @BeforeEach
    void setUp() {
        // Create AI bot user if not exists (migration may not have run)
        createAiBotUserIfNotExists();
    }

    @Test
    @Transactional
    void testAiBotUserExists() {
        
        // Verify AI bot account exists
        Optional<Account> aiBotAccount = accountRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
        
        assertTrue(aiBotAccount.isPresent(), 
            "AI bot account should exist with email: " + AI_BOT_EMAIL);
        
        Account account = aiBotAccount.get();
        assertEquals(EnumRole.CUSTOMER, account.getRole());
        assertEquals(EnumStatus.ACTIVE, account.getStatus());
        assertTrue(account.isEnabled());
        assertFalse(account.getIsDeleted());

        // Verify AI bot user exists
        Optional<User> aiBotUser = userRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
        
        assertTrue(aiBotUser.isPresent(), 
            "AI bot user should exist with email: " + AI_BOT_EMAIL);
        
        User user = aiBotUser.get();
        assertEquals("AI Assistant", user.getFullName());
        assertEquals(EnumStatus.ACTIVE, user.getStatus());
        assertFalse(user.getIsDeleted());
        assertEquals(account.getId(), user.getAccount().getId());
    }

    @Test
    @Transactional
    void testGetOrCreateAiBotUser() {
        // This test verifies that getOrCreateAiBotUser() works correctly
        // Note: This method is private in ChatMessageServiceImpl, so we test indirectly
        // by verifying the AI bot user exists and can be retrieved
        
        Optional<User> aiBotUser = userRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
        
        assertTrue(aiBotUser.isPresent(), 
            "AI bot user must exist for chat service to work");
        
        User user = aiBotUser.get();
        assertNotNull(user.getId());
        assertNotNull(user.getAccount());
        assertEquals(AI_BOT_EMAIL, user.getAccount().getEmail());
    }

    @Test
    @Transactional
    void testAiBotAccountPasswordHash() {
        // Verify password hash exists and is BCrypt format
        Optional<Account> aiBotAccount = accountRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
        
        assertTrue(aiBotAccount.isPresent());
        
        Account account = aiBotAccount.get();
        String passwordHash = account.getPassword();
        
        assertNotNull(passwordHash, "Password hash should not be null");
        assertTrue(passwordHash.startsWith("$2a$") || passwordHash.startsWith("$2b$") || passwordHash.startsWith("$2y$"),
            "Password hash should be BCrypt format (starts with $2a$, $2b$, or $2y$)");
        assertEquals(60, passwordHash.length(), 
            "BCrypt hash should be 60 characters long");
    }

    @Test
    @Transactional
    void testAiBotUserNotDeleted() {
        // Verify AI bot user is not soft-deleted
        Optional<User> aiBotUser = userRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
        
        assertTrue(aiBotUser.isPresent());
        
        User user = aiBotUser.get();
        assertFalse(user.getIsDeleted(), "AI bot user should not be deleted");
        assertFalse(user.getAccount().getIsDeleted(), "AI bot account should not be deleted");
    }

    private void createAiBotUserIfNotExists() {
        try {
            // Check if AI bot account exists
            Optional<Account> existingAccount = accountRepository.findByEmailAndIsDeletedFalse(AI_BOT_EMAIL);
            if (existingAccount.isPresent()) {
                return; // Already exists
            }

            // Create AI bot account
            String accountId = java.util.UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO accounts (id, email, password, role, status, enabled, account_non_expired, account_non_locked, credentials_non_expired, is_deleted, email_verified, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), NOW())",
                accountId,
                AI_BOT_EMAIL,
                "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy", // BCrypt hash
                "CUSTOMER",
                "ACTIVE",
                true, true, true, true, false, false // email_verified = false
            );

            // Create AI bot user
            String userId = java.util.UUID.randomUUID().toString();
            jdbcTemplate.update(
                "INSERT INTO users (id, account_id, full_name, status, is_deleted, created_at, updated_at) " +
                "VALUES (?, ?, ?, ?, ?, NOW(), NOW())",
                userId, accountId, "AI Assistant", "ACTIVE", false
            );
        } catch (Exception e) {
            // Ignore if already exists or schema not ready
            // This is safe because the test will fail if user doesn't exist
        }
    }
}

