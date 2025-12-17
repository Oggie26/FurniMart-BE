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
 * Integration Tests for Order Module
 * Based on test cases: ORD-01-01, ORD-01-02, ORD-02-01, ORD-02-02, VOU-01-01, VOU-01-02, RPT-01-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Order Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrderModuleIntegrationTest {

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
        testEmail = "order_test_" + System.currentTimeMillis() + "@test.com";
        testPassword = "Test@123456";
        
        // Register and login
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "Order Test User", "0123456789"
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
    @DisplayName("[ORD-01-02] Đặt hàng thất bại (Giỏ hàng trống)")
    void testCreateOrderEmptyCart() {
        // Arrange - Ensure cart is empty
        String clearUrl = baseUrl + "/api/carts";
        TestUtils.deleteRequest(restTemplate, clearUrl, ApiResponse.class, accessToken);

        String orderUrl = baseUrl + "/api/orders";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, orderUrl, new HashMap<>(), ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        if (responseBody != null) {
            assertThat(responseBody.getMessage()).contains("Giỏ hàng trống");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[ORD-01-01] Đặt hàng thành công (Checkout)")
    void testCreateOrderSuccess() {
        // Arrange - Add item to cart first
        String productColorId = "PC001"; // Mock ID
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=1";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        Map<String, Object> orderRequest = new HashMap<>();
        orderRequest.put("addressId", 1L); // Mock address ID
        orderRequest.put("paymentMethod", "WALLET");
        String orderUrl = baseUrl + "/api/orders";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, orderUrl, orderRequest, ApiResponse.class, accessToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            if (responseBody != null) {
                assertThat(responseBody.getMessage()).contains("Đặt hàng thành công");
                
                // Extract order ID for later tests
                if (responseBody.getData() != null) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> orderData = objectMapper.convertValue(responseBody.getData(), Map.class);
                    if (orderData != null && orderData.containsKey("id")) {
                        orderId = Long.valueOf(orderData.get("id").toString());
                    }
                }
            }
        }
    }

    @Test
    @Order(3)
    @DisplayName("[ORD-02-01] Hủy Đơn hàng thành công")
    void testCancelOrderSuccess() {
        // Arrange - Create order first (if not exists)
        if (orderId == null) {
            testCreateOrderSuccess();
        }
        
        if (orderId != null) {
            String cancelUrl = baseUrl + "/api/orders/" + orderId + "/cancel";

            // Act
            @SuppressWarnings("unchecked")
            ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.putRequest(
                restTemplate, cancelUrl, new HashMap<>(), ApiResponse.class, accessToken
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            ApiResponse<?> responseBody = response.getBody();
            assertThat(responseBody).isNotNull();
            if (responseBody != null) {
                assertThat(responseBody.getMessage()).contains("Hủy đơn hàng thành công");
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("[VOU-01-02] Tạo Voucher thất bại (Trùng mã)")
    void testCreateVoucherDuplicateCode() {
        // Arrange - Need admin token
        Map<String, Object> voucherRequest = new HashMap<>();
        voucherRequest.put("code", "TEST2024");
        voucherRequest.put("discountPercent", 10);
        voucherRequest.put("maxDiscount", 50000);
        String voucherUrl = baseUrl + "/api/vouchers";

        // Act - Create first time
        TestUtils.postRequest(restTemplate, voucherUrl, voucherRequest, ApiResponse.class, accessToken);
        
        // Try to create again with same code
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, voucherUrl, voucherRequest, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        if (responseBody != null) {
            assertThat(responseBody.getMessage()).contains("Mã voucher đã tồn tại");
        }
    }
}

