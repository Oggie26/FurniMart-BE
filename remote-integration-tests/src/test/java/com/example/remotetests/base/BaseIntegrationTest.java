package com.example.remotetests.base;

import com.example.remotetests.config.TestConfig;
import com.example.remotetests.util.AuthHelper;
import org.junit.jupiter.api.BeforeAll;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * Base class for integration tests with automatic authentication
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
public abstract class BaseIntegrationTest {

    @Autowired
    protected RestTemplate restTemplate;

    @Autowired
    protected TestConfig testConfig;

    protected String baseUrl;
    protected String customerToken;
    protected String adminToken;
    
    private static Map<String, String> sharedTokens = new HashMap<>();
    private static boolean tokensInitialized = false;

    @BeforeAll
    static void initializeTokens() {
        if (!tokensInitialized) {
            // Tokens will be initialized per test class
            tokensInitialized = true;
        }
    }

    protected void setUp() {
        baseUrl = testConfig.getUserServiceUrl();
        
        // Try to get tokens from shared pool or create new ones
        if (sharedTokens.isEmpty()) {
            initializeAuthTokens();
        }
        
        customerToken = sharedTokens.getOrDefault("customer", null);
        adminToken = sharedTokens.getOrDefault("admin", null);
    }

    private void initializeAuthTokens() {
        // Try to login with existing test accounts first
        String testCustomerEmail = System.getenv("TEST_CUSTOMER_EMAIL");
        String testCustomerPassword = System.getenv("TEST_CUSTOMER_PASSWORD");
        
        if (testCustomerEmail != null && testCustomerPassword != null) {
            customerToken = AuthHelper.login(restTemplate, baseUrl, testCustomerEmail, testCustomerPassword);
            if (customerToken != null) {
                sharedTokens.put("customer", customerToken);
            }
        }
        
        // If login failed, try to create new accounts
        if (customerToken == null) {
            Map<String, String> newTokens = AuthHelper.createTestAccounts(restTemplate, baseUrl);
            sharedTokens.putAll(newTokens);
            customerToken = newTokens.get("customer");
            adminToken = newTokens.get("admin");
        }
    }
}

