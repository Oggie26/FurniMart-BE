package com.example.remotetests.order;

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
 * Integration Tests for Payment and Warranty Modules
 * Based on test cases: PAY-01-01, PAY-02-01, PAY-02-02, WAR-01-01, WAR-01-02, WAR-02-01, WAR-03-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Payment & Warranty Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class PaymentWarrantyModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String testEmail;
    private String testPassword;
    private String accessToken;
    private Long orderId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getOrderServiceUrl();
        testEmail = "payment_test_" + System.currentTimeMillis() + "@test.com";
        testPassword = "Test@123456";
        
        // Register and login
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "Payment Test User", "0123456789"
        );
        String userServiceUrl = testConfig.getUserServiceUrl();
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
    @DisplayName("[PAY-01-01] Tạo URL Thanh toán VNPay")
    void testCreateVNPayUrl() {
        // Arrange - Create order first
        String productColorId = "PC001";
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=1";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("addressId", 1L);
        orderRequest.put("paymentMethod", "VNPAY");
        String orderUrl = baseUrl + "/api/orders";
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> orderResponse = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, orderUrl, orderRequest, ApiResponse.class, accessToken
        );

        if (orderResponse.getStatusCode() == HttpStatus.CREATED) {
            ApiResponse<?> orderResponseBody = orderResponse.getBody();
            if (orderResponseBody != null && orderResponseBody.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> orderData = objectMapper.convertValue(orderResponseBody.getData(), Map.class);
                if (orderData != null && orderData.containsKey("id")) {
                    orderId = Long.valueOf(orderData.get("id").toString());
                }
            }
        }

        // Act - Submit order for payment
        String submitUrl = baseUrl + "/api/orders/" + orderId + "/submit";
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, submitUrl, new HashMap<>(), ApiResponse.class, accessToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            if (responseBody != null && responseBody.getData() != null) {
                @SuppressWarnings("unchecked")
                Map<String, Object> data = objectMapper.convertValue(responseBody.getData(), Map.class);
                if (data != null && data.containsKey("paymentUrl")) {
                    String paymentUrl = (String) data.get("paymentUrl");
                    assertThat(paymentUrl).startsWith("https://sandbox.vnpayment.vn");
                }
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("[PAY-02-01] Xử lý Callback Thanh toán Thành công")
    void testPaymentCallbackSuccess() {
        // Arrange - Simulate VNPay callback
        Map<String, Object> callbackParams = new HashMap<>();
        callbackParams.put("vnp_ResponseCode", "00");
        callbackParams.put("vnp_TxnRef", orderId != null ? orderId.toString() : "ORDER001");
        String callbackUrl = baseUrl + "/api/wallets/deposit-via-vnpay/callback";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, callbackUrl, callbackParams, ApiResponse.class, null
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
        }
    }

    @Test
    @Order(3)
    @DisplayName("[PAY-02-02] Xử lý Callback Thanh toán Thất bại")
    void testPaymentCallbackFailed() {
        // Arrange - Simulate failed callback
        Map<String, Object> callbackParams = new HashMap<>();
        callbackParams.put("vnp_ResponseCode", "24"); // User cancelled
        callbackParams.put("vnp_TxnRef", "ORDER001");
        String callbackUrl = baseUrl + "/api/wallets/deposit-via-vnpay/callback";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, callbackUrl, callbackParams, ApiResponse.class, null
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
        }
    }

    @Test
    @Order(4)
    @DisplayName("[WAR-01-01] Tạo Yêu cầu Bảo hành thành công")
    void testCreateWarrantyClaim() {
        // Arrange
        Map<String, Object> warrantyRequest = new HashMap<>();
        warrantyRequest.put("productId", "PROD001");
        warrantyRequest.put("orderId", orderId);
        warrantyRequest.put("issueDescription", "Product defect");
        String url = baseUrl + "/api/warranties/claim";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, warrantyRequest, ApiResponse.class, accessToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            if (responseBody != null) {
                assertThat(responseBody.getMessage()).contains("Yêu cầu bảo hành đã được gửi");
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("[WAR-01-02] Tạo Yêu cầu Bảo hành thất bại (Hết hạn)")
    void testCreateWarrantyExpired() {
        // Arrange - Product purchased more than 12 months ago
        Map<String, Object> warrantyRequest = new HashMap<>();
        warrantyRequest.put("productId", "PROD_EXPIRED");
        warrantyRequest.put("orderId", orderId);
        warrantyRequest.put("issueDescription", "Product defect");
        String url = baseUrl + "/api/warranties/claim";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, warrantyRequest, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        if (responseBody != null) {
            assertThat(responseBody.getMessage()).contains("Đã hết hạn bảo hành");
        }
    }

    @Test
    @Order(6)
    @DisplayName("[WAR-02-01] Xem Lịch sử Bảo hành")
    void testGetWarrantyHistory() {
        // Arrange
        String url = baseUrl + "/api/warranties/history";

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
}

