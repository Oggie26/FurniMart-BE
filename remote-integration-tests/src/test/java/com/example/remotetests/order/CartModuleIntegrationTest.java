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
 * Integration Tests for Cart Module
 * Based on test cases: CRT-01-01, CRT-01-02, CRT-02-01, CRT-03-01, CRT-04-01, CRT-04-02, CRT-05-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Cart Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class CartModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String testEmail;
    private String testPassword;
    private String accessToken;
    private String productColorId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getOrderServiceUrl();
        testEmail = "cart_test_" + System.currentTimeMillis() + "@test.com";
        testPassword = "Test@123456";
        
        // Register and login
        Map<String, Object> registerRequest = TestUtils.createRegisterRequest(
            testEmail, testPassword, "Cart Test User", "0123456789"
        );
        String userServiceUrl = testConfig.getUserServiceUrl();
        TestUtils.postRequest(restTemplate, userServiceUrl + "/api/auth/register", 
            registerRequest, ApiResponse.class, null);

        Map<String, Object> loginRequest = TestUtils.createLoginRequest(testEmail, testPassword);
        ResponseEntity<ApiResponse> loginResponse = TestUtils.postRequest(
            restTemplate, userServiceUrl + "/api/auth/login", loginRequest, ApiResponse.class, null
        );
        
        Map<String, Object> loginData = objectMapper.convertValue(loginResponse.getBody().getData(), Map.class);
        accessToken = (String) loginData.get("accessToken");
        
        // Note: productColorId should be obtained from product service
        productColorId = "PC001"; // Mock ID
    }

    @Test
    @Order(1)
    @DisplayName("[CRT-01-01] Thêm Sản phẩm vào Giỏ hàng thành công")
    void testAddToCartSuccess() {
        // Arrange
        String url = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=2";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, null, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Thêm sản phẩm vào giỏ hàng thành công");
    }

    @Test
    @Order(2)
    @DisplayName("[CRT-01-02] Thêm Sản phẩm thất bại (Số lượng không hợp lệ)")
    void testAddToCartInvalidQuantity() {
        // Arrange
        String url = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=0";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, null, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(3)
    @DisplayName("[CRT-02-01] Lấy Chi tiết Giỏ hàng")
    void testGetCart() {
        // Arrange - Add item first
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=2";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        String getUrl = baseUrl + "/api/carts";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, getUrl, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Lấy giỏ hàng thành công");
    }

    @Test
    @Order(4)
    @DisplayName("[CRT-03-01] Cập nhật Số lượng")
    void testUpdateCartQuantity() {
        // Arrange - Add item first
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=2";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        String updateUrl = baseUrl + "/api/carts/update?productColorId=" + productColorId + "&quantity=5";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.patchRequest(
            restTemplate, updateUrl, null, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Cập nhật số lượng sản phẩm thành công");
    }

    @Test
    @Order(5)
    @DisplayName("[CRT-04-01] Xóa một Sản phẩm")
    void testRemoveCartItem() {
        // Arrange - Add item first
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=2";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        String removeUrl = baseUrl + "/api/carts/remove/" + productColorId;

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.deleteRequest(
            restTemplate, removeUrl, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Xoá sản phẩm khỏi giỏ hàng thành công");
    }

    @Test
    @Order(6)
    @DisplayName("[CRT-05-01] Xóa toàn bộ Giỏ hàng")
    void testClearCart() {
        // Arrange - Add items first
        String addUrl = baseUrl + "/api/carts/add?productColorId=" + productColorId + "&quantity=2";
        TestUtils.postRequest(restTemplate, addUrl, null, ApiResponse.class, accessToken);

        String clearUrl = baseUrl + "/api/carts";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.deleteRequest(
            restTemplate, clearUrl, ApiResponse.class, accessToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getMessage()).contains("Dọn giỏ hàng thành công");
    }
}

