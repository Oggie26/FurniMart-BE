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
 * Integration Tests for User Module
 * Based on test cases: USR-01-01, USR-01-02, USR-02-01, USR-02-02, USR-03-01, USR-03-02
 * 
 * Uses PER_CLASS lifecycle to share authentication token between test methods.
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("User Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@SuppressWarnings({"rawtypes", "null"})
public class UserModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    
    // Shared state for authentication
    private String accessToken;
    private String testEmail;
    private String testPassword;

    /**
     * Generate a unique phone number using timestamp
     * Format: 0 + (timestamp % 1000000000) to ensure 10 digits starting with 0
     */
    private String generateUniquePhone() {
        long timestamp = System.currentTimeMillis();
        String phoneSuffix = String.format("%09d", timestamp % 1000000000L);
        return "0" + phoneSuffix;
    }

    @BeforeAll
    void setUpAuthentication() {
        baseUrl = testConfig.getUserServiceUrl();
        
        // Generate unique credentials
        long timestamp = System.currentTimeMillis();
        testEmail = "user_test_" + timestamp + "@test.com";
        testPassword = "Test@123456";
        
        // Step 1: Register a new user
        String uniquePhone = generateUniquePhone();
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "Test User", uniquePhone
        );
        String registerUrl = baseUrl + "/api/auth/register";
        
        ResponseEntity<ApiResponse> registerResponse = TestUtils.postRequest(
            restTemplate, registerUrl, registerRequest, ApiResponse.class, null
        );
        
        // Assert registration success
        assertThat(registerResponse.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(registerResponse.getBody()).isNotNull();
        
        // Step 2: Login to get access token
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(testEmail, testPassword);
        String loginUrl = baseUrl + "/api/auth/login";
        
        ResponseEntity<ApiResponse> loginResponse = TestUtils.postRequest(
            restTemplate, loginUrl, loginRequest, ApiResponse.class, null
        );
        
        // Assert login success
        assertThat(loginResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(loginResponse.getBody()).isNotNull();
        assertThat(loginResponse.getBody().getData()).isNotNull();
        
        // Step 3: Extract accessToken from login response
        @SuppressWarnings("unchecked")
        Map<String, Object> loginData = objectMapper.convertValue(loginResponse.getBody().getData(), Map.class);
        accessToken = (String) loginData.get("token"); // API returns "token" not "accessToken"
        
        // Verify token was extracted
        assertThat(accessToken).isNotNull();
        assertThat(accessToken).isNotEmpty();
    }

    @Test
    @Order(1)
    @DisplayName("[USR-01-01] Lấy Hồ sơ Người dùng thành công")
    void testGetProfileSuccess() {
        // Arrange
        String url = baseUrl + "/api/users/profile";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getData()).isNotNull();
        
        // Verify body contains user info
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = objectMapper.convertValue(response.getBody().getData(), Map.class);
        assertThat(userData).isNotNull();
        // Verify common user fields exist
        assertThat(userData.containsKey("id") || userData.containsKey("fullName") || 
                   userData.containsKey("email") || userData.containsKey("phone")).isTrue();
    }

    @Test
    @Order(2)
    @DisplayName("[USR-01-02] Lấy Hồ sơ thất bại (Không có Token)")
    void testGetProfileUnauthorized() {
        // Arrange - Do NOT use token
        String url = baseUrl + "/api/users/profile";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.getRequest(
                restTemplate, url, ApiResponse.class, null
            );
            // If no exception, check status code (may be 401 or 404 depending on routing)
            assertThat(response.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for unauthorized request (may be 401 or 404)
            assertThat(e.getStatusCode()).isIn(HttpStatus.UNAUTHORIZED, HttpStatus.NOT_FOUND);
        }
    }

    @Test
    @Order(3)
    @DisplayName("[USR-02-01] Cập nhật Hồ sơ Người dùng thành công")
    void testUpdateProfileSuccess() {
        // Arrange - Use accessToken, send PUT with new Name and unique phone number
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("fullName", "Updated Test User Name");
        updateRequest.put("phone", generateUniquePhone());
        String url = baseUrl + "/api/users/profile";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.putRequest(
            restTemplate, url, updateRequest, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(200);
        assertThat(response.getBody().getData()).isNotNull();
        
        // Verify updated data
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = objectMapper.convertValue(response.getBody().getData(), Map.class);
        assertThat(userData.get("fullName")).isEqualTo("Updated Test User Name");
    }

    @Test
    @Order(4)
    @DisplayName("[USR-02-02] Cập nhật Hồ sơ thất bại (Dữ liệu không hợp lệ)")
    void testUpdateProfileInvalid() {
        // Arrange - Use accessToken, send PUT with empty fullName (invalid data)
        // Expected: Server should reject and return 400 Bad Request
        Map<String, Object> updateRequest = new HashMap<>();
        updateRequest.put("fullName", ""); // Empty name should be invalid
        String url = baseUrl + "/api/users/profile";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.putRequest(
                restTemplate, url, updateRequest, ApiResponse.class, accessToken
            );
            
            // CRITICAL: Server MUST return 400 BAD_REQUEST for invalid data
            // If server returns 200 OK, this is a BUG and test should FAIL
            assertThat(response.getStatusCode())
                .as("Server should reject invalid data (empty fullName) with 400 Bad Request, but got: %s. This indicates a validation bug.",
                     response.getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
                
            // Verify error message indicates validation failure
            if (response.getBody() != null) {
                assertThat(response.getBody().getMessage())
                    .as("Error message should indicate validation failure")
                    .isNotNull();
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for invalid data
            assertThat(e.getStatusCode())
                .as("Server should reject invalid data with 400 Bad Request")
                .isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }

    @Test
    @Order(5)
    @DisplayName("[USR-03-01] Tạo Địa chỉ Giao hàng mới thành công")
    void testCreateAddressSuccess() {
        // Arrange - Use accessToken, send POST with valid Address DTO
        // First, get user profile to extract userId
        String profileUrl = baseUrl + "/api/users/profile";
        ResponseEntity<ApiResponse> profileResponse = TestUtils.getRequest(
            restTemplate, profileUrl, ApiResponse.class, accessToken
        );
        assertThat(profileResponse.getBody()).isNotNull();
        @SuppressWarnings("unchecked")
        Map<String, Object> userData = objectMapper.convertValue(profileResponse.getBody().getData(), Map.class);
        String userId = (String) userData.get("id");
        
        // Create valid Address DTO
        Map<String, Object> addressRequest = new HashMap<>();
        addressRequest.put("name", "Test User");
        addressRequest.put("phone", generateUniquePhone());
        addressRequest.put("city", "Ho Chi Minh City");
        addressRequest.put("district", "District 1");
        addressRequest.put("ward", "Ward 1");
        addressRequest.put("street", "123 Test Street");
        addressRequest.put("addressLine", "123 Test Street, Ward 1, District 1, Ho Chi Minh City");
        addressRequest.put("isDefault", true);
        addressRequest.put("userId", userId);
        addressRequest.put("latitude", 10.8231);
        addressRequest.put("longitude", 106.6297);
        
        String url = baseUrl + "/api/addresses";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, addressRequest, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo(201);
        assertThat(response.getBody().getData()).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("[USR-03-02] Tạo Địa chỉ thất bại (Thiếu trường dữ liệu)")
    void testCreateAddressMissingFields() {
        // Arrange - Use accessToken, send POST with empty body (missing required fields)
        Map<String, Object> addressRequest = new HashMap<>();
        // Empty body - missing all required fields
        String url = baseUrl + "/api/addresses";

        // Act & Assert
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, url, addressRequest, ApiResponse.class, accessToken
            );
            // If no exception, check status code
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            // Expected exception for missing required fields
            assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        }
    }
}
