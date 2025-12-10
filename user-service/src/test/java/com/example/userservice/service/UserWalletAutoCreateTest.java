package com.example.userservice.service;

import com.example.userservice.entity.Account;
import com.example.userservice.entity.User;
import com.example.userservice.entity.Wallet;
import com.example.userservice.enums.EnumRole;
import com.example.userservice.enums.EnumStatus;
import com.example.userservice.enums.WalletStatus;
import com.example.userservice.repository.AccountRepository;
import com.example.userservice.repository.UserRepository;
import com.example.userservice.repository.WalletRepository;
import com.example.userservice.request.UserRequest;
import com.example.userservice.service.inteface.UserService;
import com.example.userservice.service.inteface.WalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(properties = {
    "eureka.client.enabled=false",
    "spring.kafka.enabled=false",
    "app.kafka.enabled=false"
})
@ActiveProfiles("test")
@DisplayName("User Wallet Auto-Create Tests")
class UserWalletAutoCreateTest {

    @Autowired
    private UserService userService;

    @Autowired
    private WalletService walletService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private WalletRepository walletRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        // Clean up test data (no @Transactional to allow REQUIRES_NEW to see committed data)
        walletRepository.deleteAll();
        userRepository.deleteAll();
        accountRepository.deleteAll();
        walletRepository.flush();
        userRepository.flush();
        accountRepository.flush();
    }

    @Test
    @DisplayName("Should auto-create wallet when creating new customer user")
    void testCreateUser_ShouldAutoCreateWallet() {
        // Given
        UserRequest request = UserRequest.builder()
                .email("testcustomer@example.com")
                .password("password123")
                .fullName("Test Customer")
                .phone("0123456789")
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .build();

        // When
        var response = userService.createUser(request);

        // Then - Verify user was created
        assertNotNull(response);
        assertNotNull(response.getId());
        assertEquals("Test Customer", response.getFullName());
        assertEquals("testcustomer@example.com", response.getEmail());

        // Verify account was created
        Account account = accountRepository.findByEmailAndIsDeletedFalse("testcustomer@example.com")
                .orElseThrow(() -> new AssertionError("Account should be created"));
        assertEquals(EnumRole.CUSTOMER, account.getRole());

        // Verify user was created
        User user = userRepository.findByIdAndIsDeletedFalse(response.getId())
                .orElseThrow(() -> new AssertionError("User should be created"));

        // Wait a bit for REQUIRES_NEW transaction to commit (if needed)
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        // Verify wallet was auto-created
        // Check all wallets to see if any exist
        List<Wallet> allWallets = walletRepository.findAll();
        System.out.println("All wallets in DB: " + allWallets.size());
        
        Optional<Wallet> wallet = walletRepository.findByUserIdAndIsDeletedFalse(user.getId());
        if (!wallet.isPresent()) {
            // Try to find by userId without isDeleted check
            Optional<Wallet> walletAny = walletRepository.findByUserId(user.getId());
            System.out.println("Wallet found (any status): " + walletAny.isPresent());
            if (walletAny.isPresent()) {
                System.out.println("Wallet isDeleted: " + walletAny.get().getIsDeleted());
            }
        }
        
        assertTrue(wallet.isPresent(), 
                "Wallet should be auto-created for new customer. User ID: " + user.getId() + 
                ". Total wallets in DB: " + allWallets.size());

        Wallet createdWallet = wallet.get();
        assertEquals(user.getId(), createdWallet.getUserId());
        assertEquals(0.0, createdWallet.getBalance().doubleValue(), 0.01);
        assertEquals(WalletStatus.ACTIVE, createdWallet.getStatus());
        assertNotNull(createdWallet.getCode());
        assertTrue(createdWallet.getCode().startsWith("WLT-"));
        assertFalse(createdWallet.getIsDeleted());
    }

    @Test
    @DisplayName("Should create wallet directly using WalletService")
    void testCreateWalletDirectly() {
        // Given - Create user manually
        Account account = Account.builder()
                .email("directtest@example.com")
                .password(passwordEncoder.encode("password123"))
                .role(EnumRole.CUSTOMER)
                .status(EnumStatus.ACTIVE)
                .build();
        account = accountRepository.save(account);
        accountRepository.flush();

        User user = User.builder()
                .fullName("Direct Test User")
                .account(account)
                .status(EnumStatus.ACTIVE)
                .build();
        user = userRepository.save(user);
        userRepository.flush();

        // When - Create wallet directly
        var walletResponse = walletService.createWalletForUser(user.getId());

        // Then
        assertNotNull(walletResponse);
        assertNotNull(walletResponse.getId());
        assertEquals(user.getId(), walletResponse.getUserId());
        assertNotNull(walletResponse.getBalance());
        assertEquals(0.0, walletResponse.getBalance().doubleValue(), 0.01);
        assertEquals(WalletStatus.ACTIVE, walletResponse.getStatus());

        // Verify in database
        Optional<Wallet> wallet = walletRepository.findByUserIdAndIsDeletedFalse(user.getId());
        assertTrue(wallet.isPresent(), "Wallet should exist in database");
    }
}
