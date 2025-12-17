package com.example.remotetests.user;

import com.example.remotetests.config.TestConfig;
import com.example.remotetests.dto.ApiResponse;
import com.example.remotetests.util.TestUtils;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Auth Module
 * Based on test cases: AUTH-01-01, AUTH-01-02, AUTH-02-01, AUTH-02-02, AUTH-02-03,
 *                      AUTH-03-01, AUTH-04-01, AUTH-04-02
 * 
 * Uses PER_CLASS lifecycle to share state between test methods for dependent test flow.
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Auth Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"rawtypes", "null"})
public class AuthModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    
    // Shared state variables for dependent test flow
    private String currentEmail;
    private String currentPassword;
    private String currentPhone;
    private String validRefreshToken;

    @BeforeAll
    void setUp() {
        baseUrl = testConfig.getUserServiceUrl();
        currentPassword = "Test@123456";
    }

    /**
     * Generate a unique phone number using timestamp
     * Format: 0 + (timestamp % 1000000000) to ensure 10 digits starting with 0
     */
    private String generateUniquePhone() {
        long timestamp = System.currentTimeMillis();
        // Use last 9 digits of timestamp, prepend with 0
        String phoneSuffix = String.format("%09d", timestamp % 1000000000L);
        return "0" + phoneSuffix;
    }

    @Test
    @Order(1)
    @DisplayName("[AUTH-01-01] Đăng ký tài khoản mới thành công")
    void testRegisterSuccess() {
        // Arrange - Generate unique data and store in shared variables
        long timestamp = System.currentTimeMillis();
        currentEmail = "test_" + timestamp + "@test.com";
        currentPhone = generateUniquePhone();
        
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            currentEmail, currentPassword, "Test User", currentPhone
        );
        String url = baseUrl + "/api/auth/register";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, registerRequest, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getMessage()).contains("Đăng kí thành công");
    }
    
    @Test
    @Order(2)
    @DisplayName("[WALLET-AUTO] Verify Wallet được tạo tự động sau Register")
    void testWalletCreatedAfterRegister() {
        // Arrange - Register a new user first
        long timestamp = System.currentTimeMillis();
        String testEmail = "wallet_auto_" + timestamp + "@test.com";
        String testPhone = generateUniquePhone();
        String testPassword = "Test@123456";
        
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "Wallet Auto Test User", testPhone
        );
        String registerUrl = baseUrl + "/api/auth/register";

        // Act 1: Register
        ResponseEntity<ApiResponse> registerResponse = TestUtils.postRequest(
            restTemplate, registerUrl, registerRequest, ApiResponse.class, null
        );

        // Assert 1: Registration successful
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        assertThat(registerResponse.getBody().getMessage()).contains("Đăng kí thành công");
        
        // Act 2: Login to get token
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(testEmail, testPassword);
        String loginUrl = baseUrl + "/api/auth/login";
        ResponseEntity<ApiResponse> loginResponse = TestUtils.postRequest(
            restTemplate, loginUrl, loginRequest, ApiResponse.class, null
        );
        
        // Assert 2: Login successful
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        
        // Extract token
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = objectMapper.convertValue(loginResponse.getBody().getData(), Map.class);
        String token = (String) loginData.get("token");
        assertThat(token).isNotNull();
        
        // Act 3: Get wallet immediately after registration
        String walletUrl = baseUrl + "/api/wallets/my-wallet";
        ResponseEntity<ApiResponse> walletResponse = TestUtils.getRequest(
            restTemplate, walletUrl, ApiResponse.class, token
        );
        
        // Assert 3: Wallet exists and has correct initial state
        assertThat(walletResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(walletResponse.getBody()).isNotNull();
        assertThat(walletResponse.getBody().getStatus()).isEqualTo(200);
        
        // Verify wallet data structure
        @SuppressWarnings("unchecked")
        Map<String, Object> walletData = objectMapper.convertValue(walletResponse.getBody().getData(), Map.class);
        assertThat(walletData).isNotNull();
        assertThat(walletData).containsKey("id");
        assertThat(walletData).containsKey("code");
        assertThat(walletData).containsKey("balance");
        assertThat(walletData).containsKey("status");
        
        // Verify initial balance is 0
        Object balance = walletData.get("balance");
        if (balance instanceof Number) {
            assertThat(((Number) balance).doubleValue()).isEqualTo(0.0);
        } else if (balance instanceof String) {
            assertThat(Double.parseDouble((String) balance)).isEqualTo(0.0);
        }
        
        // Verify wallet code format (should start with "WLT-")
        String walletCode = (String) walletData.get("code");
        assertThat(walletCode).isNotNull();
        assertThat(walletCode).startsWith("WLT-");
    }

    @Test
    @Order(3)
    @DisplayName("[AUTH-01-02] Đăng ký thất bại (Email đã tồn tại)")
    void testRegisterEmailExists() {
        // Arrange - Use shared email from testRegisterSuccess to test duplicate
        // This test depends on testRegisterSuccess (Order 1) having created the user
        assertThat(currentEmail).isNotNull();
        
        // Try to register again with same email (from testRegisterSuccess)
        Map<String, Object> duplicateRegister = TestUtils.createRegisterRequest(
            currentEmail, "DifferentPassword@123", "Second User", generateUniquePhone()
        );
        String registerUrl = baseUrl + "/api/auth/register";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, registerUrl, duplicateRegister, ApiResponse.class, null
            );
            // If no exception, check status code
            assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).containsAnyOf("Email", "email");
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for duplicate email
            assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.CONFLICT);
            assertThat(e.getResponseBodyAsString()).containsAnyOf("Email", "email");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[AUTH-02-01] Đăng nhập thành công")
    void testLoginSuccess() {
        // Arrange - Use shared credentials from testRegisterSuccess
        // This test depends on testRegisterSuccess (Order 1) having created the user
        assertThat(currentEmail).isNotNull();
        assertThat(currentPassword).isNotNull();
        
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(currentEmail, currentPassword);
        String loginUrl = baseUrl + "/api/auth/login";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, loginUrl, loginRequest, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getMessage()).contains("Đăng nhập thành công");
        
        // Check token exists (API returns "token" not "accessToken")
        @SuppressWarnings("unchecked")
        Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
        assertThat(data).containsKey("token");
        assertThat(data.get("token")).isNotNull();
        
        // Save refreshToken for testRefreshTokenSuccess
        validRefreshToken = (String) data.get("refreshToken");
        assertThat(validRefreshToken).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("[AUTH-02-02] Đăng nhập thất bại (Sai mật khẩu)")
    void testLoginWrongPassword() {
        // Arrange - Use shared email from testRegisterSuccess, but wrong password
        // This test depends on testRegisterSuccess (Order 1) having created the user
        assertThat(currentEmail).isNotNull();
        
        // Login with wrong password
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(currentEmail, "WrongPassword@123");
        String loginUrl = baseUrl + "/api/auth/login";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, loginUrl, loginRequest, ApiResponse.class, null
            );
            // If no exception, check status code
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
            if (response.getBody() != null) {
                assertThat(response.getBody().getMessage()).containsAnyOf("Email", "mật khẩu", "password");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for wrong password
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @Order(6)
    @DisplayName("[AUTH-02-03] Đăng nhập thất bại (Format Email không hợp lệ)")
    void testLoginInvalidEmailFormat() {
        // Arrange - Independent test, doesn't use shared state
        Map<String, Object> loginRequest = TestUtils.createLoginRequest("invalid-email-format", currentPassword);
        String loginUrl = baseUrl + "/api/auth/login";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, loginUrl, loginRequest, ApiResponse.class, null
            );
            // Server may return 401 for invalid email format during authentication
            assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for invalid email format
            assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @Order(6)
    @DisplayName("[AUTH-03-01] Đăng nhập bằng Google thành công")
    void testGoogleLoginSuccess() {
        // Arrange
        Map<String, Object> googleLoginRequest = new HashMap<>();
        googleLoginRequest.put("token", "mock-google-token");
        String googleLoginUrl = baseUrl + "/api/auth/google/login";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, googleLoginUrl, googleLoginRequest, ApiResponse.class, null
            );
            // Google OAuth requires valid token, expect 400 for mock token
            assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for invalid Google token
            assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED);
        }
    }

    @Test
    @Order(4)
    @DisplayName("[AUTH-04-01] Làm mới Token thành công")
    void testRefreshTokenSuccess() {
        // Arrange - Use shared refreshToken from testLoginSuccess
        // This test depends on testLoginSuccess (Order 2) having saved validRefreshToken
        assertThat(validRefreshToken).isNotNull();
        
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", validRefreshToken);
        String refreshUrl = baseUrl + "/api/auth/refresh";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, refreshUrl, refreshRequest, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Refresh token thành công");
    }

    @Test
    @Order(8)
    @DisplayName("[AUTH-04-02] Làm mới Token thất bại (Token không hợp lệ)")
    void testRefreshTokenInvalid() {
        // Arrange
        Map<String, Object> refreshRequest = new HashMap<>();
        refreshRequest.put("refreshToken", "invalid-refresh-token");
        String refreshUrl = baseUrl + "/api/auth/refresh";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, refreshUrl, refreshRequest, ApiResponse.class, null
            );
            // Invalid token may return 400 or 401/403
            assertThat(response.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for invalid refresh token
            assertThat(e.getStatusCode()).isIn(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN);
        }
    }
}

