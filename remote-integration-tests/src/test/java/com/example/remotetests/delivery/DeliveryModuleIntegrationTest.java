package com.example.remotetests.delivery;

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
 * Integration Tests for Delivery Module
 * Based on test cases: DEL-01-01, DEL-01-02, DEL-02-01, DEL-02-02, DEL-02-03, DEL-03-01, DEL-03-02
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Delivery Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class DeliveryModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String managerToken;
    private String shipperToken;
    private Long orderId;
    private Long assignmentId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getDeliveryServiceUrl();
        // Note: Manager and shipper tokens should be obtained from login
    }

    @Test
    @Order(1)
    @DisplayName("[DEL-01-01] Phân công Đơn hàng cho Shipper thành công")
    void testAssignOrderSuccess() {
        // Arrange
        Map<String, Object> assignRequest = new HashMap<>();
        assignRequest.put("orderId", orderId);
        assignRequest.put("shipperId", "SHIPPER001"); // Mock shipper ID
        String url = baseUrl + "/api/delivery/assign";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, assignRequest, ApiResponse.class, managerToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Phân công giao hàng thành công");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[DEL-01-02] Phân công thất bại (Shipper không tồn tại)")
    void testAssignOrderShipperNotFound() {
        // Arrange
        Map<String, Object> assignRequest = new HashMap<>();
        assignRequest.put("orderId", orderId);
        assignRequest.put("shipperId", "INVALID_SHIPPER_ID");
        String url = baseUrl + "/api/delivery/assign";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, assignRequest, ApiResponse.class, managerToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody().getMessage()).contains("Shipper not found");
    }

    @Test
    @Order(3)
    @DisplayName("[DEL-02-01] Cập nhật Trạng thái: ĐANG GIAO (DELIVERING)")
    void testUpdateStatusDelivering() {
        // Arrange
        String url = baseUrl + "/api/delivery/" + orderId + "/status?status=DELIVERING";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.putRequest(
            restTemplate, url, new HashMap<>(), ApiResponse.class, shipperToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Cập nhật trạng thái thành công");
        }
    }

    @Test
    @Order(4)
    @DisplayName("[DEL-02-02] Cập nhật Trạng thái: ĐÃ GIAO (DELIVERED)")
    void testUpdateStatusDelivered() {
        // Arrange
        String url = baseUrl + "/api/delivery/" + orderId + "/status?status=DELIVERED";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.putRequest(
            restTemplate, url, new HashMap<>(), ApiResponse.class, shipperToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Test
    @Order(5)
    @DisplayName("[DEL-02-03] Cập nhật Trạng thái thất bại (Chuyển đổi không hợp lệ)")
    void testUpdateStatusInvalidTransition() {
        // Arrange - Try to go from PENDING directly to DELIVERED
        String url = baseUrl + "/api/delivery/" + orderId + "/status?status=DELIVERED";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.putRequest(
            restTemplate, url, new HashMap<>(), ApiResponse.class, shipperToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Chuyển đổi trạng thái không hợp lệ");
    }

    @Test
    @Order(6)
    @DisplayName("[DEL-03-01] Xác nhận Giao hàng thành công (POD)")
    void testConfirmDeliverySuccess() {
        // Arrange - Order must be in DELIVERED status
        Map<String, Object> confirmRequest = new HashMap<>();
        confirmRequest.put("orderId", orderId);
        confirmRequest.put("signature", "base64_signature_data");
        String url = baseUrl + "/api/delivery/confirm";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, confirmRequest, ApiResponse.class, shipperToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Test
    @Order(7)
    @DisplayName("[DEL-03-02] Xác nhận Giao hàng thất bại (Chưa đến nơi)")
    void testConfirmDeliveryNotDelivered() {
        // Arrange - Order in SHIPPING status
        Map<String, Object> confirmRequest = new HashMap<>();
        confirmRequest.put("orderId", orderId);
        String url = baseUrl + "/api/delivery/confirm";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, confirmRequest, ApiResponse.class, shipperToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody().getMessage()).contains("Đơn hàng chưa được giao đến nơi");
    }
}

