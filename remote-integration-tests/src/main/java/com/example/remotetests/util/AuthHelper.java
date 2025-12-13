package com.example.remotetests.util;

import com.example.remotetests.dto.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

public class AuthHelper {
    
    private static final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Đăng ký và đăng nhập tự động, trả về access token
     */
    public static String getAuthToken(RestTemplate restTemplate, String baseUrl, 
                                      String email, String password, String fullName) {
        try {
            // Step 1: Register
            Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
                email, password, fullName, "0123456789"
            );
            String registerUrl = baseUrl + "/api/auth/register";
            TestUtils.postRequest(restTemplate, registerUrl, registerRequest, ApiResponse.class, null);
        } catch (Exception e) {
            // Account might already exist, try to login
        }
        
        // Step 2: Login
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(email, password);
        String loginUrl = baseUrl + "/api/auth/login";
        
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, loginUrl, loginRequest, ApiResponse.class, null
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
                if (data != null && data.containsKey("accessToken")) {
                    return (String) data.get("accessToken");
                }
            }
        } catch (Exception e) {
            // Login failed
        }
        
        return null;
    }
    
    /**
     * Đăng nhập với account có sẵn
     */
    public static String login(RestTemplate restTemplate, String baseUrl, 
                               String email, String password) {
        Map<String, Object> loginRequest = TestUtils.createLoginRequest(email, password);
        String loginUrl = baseUrl + "/api/auth/login";
        
        try {
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, loginUrl, loginRequest, ApiResponse.class, null
            );
            
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
                if (data != null && data.containsKey("accessToken")) {
                    return (String) data.get("accessToken");
                }
            }
        } catch (Exception e) {
            // Login failed
        }
        
        return null;
    }
    
    /**
     * Tạo test accounts nếu chưa tồn tại
     */
    public static Map<String, String> createTestAccounts(RestTemplate restTemplate, String baseUrl) {
        Map<String, String> tokens = new HashMap<>();
        
        // Test accounts
        Map<String, Map<String, String>> accounts = new HashMap<>();
        accounts.put("customer", Map.of(
            "email", "test_customer_" + System.currentTimeMillis() + "@test.com",
            "password", "Test@123456",
            "fullName", "Test Customer"
        ));
        accounts.put("admin", Map.of(
            "email", "test_admin_" + System.currentTimeMillis() + "@test.com",
            "password", "Admin@123456",
            "fullName", "Test Admin"
        ));
        
        for (Map.Entry<String, Map<String, String>> entry : accounts.entrySet()) {
            String role = entry.getKey();
            Map<String, String> account = entry.getValue();
            
            String token = getAuthToken(restTemplate, baseUrl, 
                account.get("email"), account.get("password"), account.get("fullName"));
            
            if (token != null) {
                tokens.put(role, token);
            }
        }
        
        return tokens;
    }
}

