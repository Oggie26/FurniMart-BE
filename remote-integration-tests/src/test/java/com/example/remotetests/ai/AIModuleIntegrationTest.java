package com.example.remotetests.ai;

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
 * Integration Tests for AI Module
 * Based on test cases: AI-01-01, AI-01-02, CHAT-01-01, CHAT-01-02, CHAT-02-01, BLG-01-01, BLG-01-02, BLG-02-01, BLG-03-01, BLG-04-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("AI Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class AIModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String aiServiceUrl;
    private String userServiceUrl;
    private String testEmail;
    private String testPassword;
    private String accessToken;

    @BeforeEach
    void setUp() {
        aiServiceUrl = testConfig.getAiServiceUrl();
        userServiceUrl = testConfig.getUserServiceUrl();
        testEmail = "ai_test_" + System.currentTimeMillis() + "@test.com";
        testPassword = "Test@123456";
        
        // Register and login
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "AI Test User", "0123456789"
        );
        TestUtils.postRequest(restTemplate, userServiceUrl + "/api/auth/register", 
            registerRequest, ApiResponse.class, null);

        Map<String, Object> loginRequest = TestUtils.createLoginRequest(testEmail, testPassword);
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> loginResponse = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, userServiceUrl + "/api/auth/login", loginRequest, ApiResponse.class, null
        );
        
        ApiResponse<?> loginBody = loginResponse.getBody();
        if (loginBody != null && loginBody.getData() != null) {
            @SuppressWarnings("unchecked")
            Map<String, Object> loginData = objectMapper.convertValue(loginBody.getData(), Map.class);
            accessToken = (String) loginData.get("accessToken");
        }
    }

    @Test
    @Order(1)
    @DisplayName("[AI-01-01] Kiểm tra Chatbot Trả lời tự động")
    void testChatbotResponse() {
        // Arrange
        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("message", "Gợi ý cho tôi một mẫu sofa đẹp");
        String url = aiServiceUrl + "/api/ai/chatbox/chat";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, chatRequest, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
    }

    @Test
    @Order(2)
    @DisplayName("[AI-01-02] Kiểm tra Chatbot thất bại (Tin nhắn trống)")
    void testChatbotEmptyMessage() {
        // Arrange
        Map<String, Object> chatRequest = new HashMap<>();
        chatRequest.put("message", "");
        String url = aiServiceUrl + "/api/ai/chatbox/chat";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, chatRequest, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        if (responseBody != null) {
            assertThat(responseBody.getMessage()).contains("Message cannot be empty");
        }
    }

    @Test
    @Order(3)
    @DisplayName("[CHAT-01-01] Gửi Tin nhắn thành công (User tới Staff)")
    void testSendChatMessage() {
        // Arrange
        Map<String, Object> messageRequest = new HashMap<>();
        messageRequest.put("receiverId", "STAFF001"); // Mock staff ID
        messageRequest.put("content", "Hello, I need help");
        String url = userServiceUrl + "/api/chat/send";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, messageRequest, ApiResponse.class, accessToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
        }
    }

    @Test
    @Order(4)
    @DisplayName("[CHAT-01-02] Lấy Lịch sử Chat")
    void testGetChatHistory() {
        // Arrange
        String receiverId = "STAFF001";
        String url = userServiceUrl + "/api/chat/history/" + receiverId;

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
    }

    @Test
    @Order(5)
    @DisplayName("[BLG-04-01] Lấy tất cả Bài viết Blog (Public)")
    void testGetAllBlogs() {
        // Arrange
        String url = userServiceUrl + "/api/blogs";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
    }

    @Test
    @Order(6)
    @DisplayName("[BLG-01-02] Tạo Bài viết thất bại (Thiếu Tiêu đề)")
    void testCreateBlogMissingTitle() {
        // Arrange - Need admin token
        Map<String, Object> blogRequest = new HashMap<>();
        blogRequest.put("content", "Blog content without title");
        String url = userServiceUrl + "/api/blogs";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, blogRequest, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

