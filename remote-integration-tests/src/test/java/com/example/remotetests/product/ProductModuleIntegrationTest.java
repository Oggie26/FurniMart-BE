package com.example.remotetests.product;

import com.example.remotetests.config.TestConfig;
import com.example.remotetests.dto.ApiResponse;
import com.example.remotetests.util.TestUtils;
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
 * Integration Tests for Product Module
 * Based on test cases: PROD-01-01, PROD-01-02, PROD-02-01, PROD-02-02, PROD-02-03,
 *                      PROD-03-01, PROD-04-01, CAT-01-01, CAT-01-02, CAT-02-01, CAT-03-01,
 *                      COL-01-01, MAT-01-01, MAT-02-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Product Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class ProductModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private String baseUrl;
    private String adminToken;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getProductServiceUrl();
        // Note: Admin token should be obtained from login
        // For now, we'll test public endpoints or skip auth-required tests
    }

    @Test
    @Order(1)
    @DisplayName("[PROD-02-01] Lấy tất cả Sản phẩm (Public API)")
    void testGetAllProducts() {
        // Arrange
        String url = baseUrl + "/api/products";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, null
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        ApiResponse<?> responseBody = response.getBody();
        assertThat(responseBody).isNotNull();
        if (responseBody != null) {
            assertThat(responseBody.getMessage()).contains("Lấy danh sách sản phẩm thành công");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[PROD-02-03] Tìm kiếm Sản phẩm theo Từ khóa")
    void testSearchProducts() {
        // Arrange
        String url = baseUrl + "/api/products/search?request=Sofa";

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
    @Order(3)
    @DisplayName("[MAT-02-01] Lấy tất cả Chất liệu")
    void testGetAllMaterials() {
        // Arrange
        String url = baseUrl + "/api/materials";

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
    @Order(4)
    @DisplayName("[PROD-01-02] Tạo Sản phẩm thất bại (Lỗi Validation)")
    void testCreateProductValidationError() {
        // Arrange
        Map<String, Object> productRequest = new HashMap<>();
        productRequest.put("name", ""); // Empty name
        productRequest.put("price", -100); // Negative price
        String url = baseUrl + "/api/products";

        // Act
        @SuppressWarnings("unchecked")
        ResponseEntity<ApiResponse<?>> response = (ResponseEntity<ApiResponse<?>>) (ResponseEntity<?>) TestUtils.postRequest(
            restTemplate, url, productRequest, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}

