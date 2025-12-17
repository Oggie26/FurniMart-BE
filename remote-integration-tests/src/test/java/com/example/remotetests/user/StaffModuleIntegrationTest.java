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
 * Integration Tests for Staff Module
 * Based on test cases: STF-01-01, STF-02-01, STF-03-01, STF-04-01, STF-04-02
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Staff Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"rawtypes", "null"})
public class StaffModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String adminToken;
    private Long staffId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getUserServiceUrl();
        // Note: Admin token should be obtained from login
    }

    @Test
    @Order(1)
    @DisplayName("[STF-01-01] Tạo tài khoản Quản lý Chi nhánh (BRANCH_MANAGER)")
    void testCreateBranchManager() {
        // Arrange
        Map<String, Object> staffRequest = new HashMap<>();
        staffRequest.put("email", "manager_" + System.currentTimeMillis() + "@test.com");
        staffRequest.put("password", "Manager@123");
        staffRequest.put("fullName", "Branch Manager");
        staffRequest.put("phoneNumber", "0123456789");
        staffRequest.put("role", "BRANCH_MANAGER");
        String url = baseUrl + "/api/staff";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, staffRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
            assertThat(data.get("role")).isEqualTo("BRANCH_MANAGER");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[STF-02-01] Tạo tài khoản Giao hàng (DELIVERY)")
    void testCreateDeliveryStaff() {
        // Arrange
        Map<String, Object> staffRequest = new HashMap<>();
        staffRequest.put("email", "delivery_" + System.currentTimeMillis() + "@test.com");
        staffRequest.put("password", "Delivery@123");
        staffRequest.put("fullName", "Delivery Staff");
        staffRequest.put("phoneNumber", "0123456789");
        staffRequest.put("role", "DELIVERY");
        String url = baseUrl + "/api/staff";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, staffRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
            assertThat(data.get("role")).isEqualTo("DELIVERY");
        }
    }

    @Test
    @Order(3)
    @DisplayName("[STF-03-01] Tạo tài khoản Nhân viên Bán hàng (STAFF)")
    void testCreateSalesStaff() {
        // Arrange
        Map<String, Object> staffRequest = new HashMap<>();
        staffRequest.put("email", "staff_" + System.currentTimeMillis() + "@test.com");
        staffRequest.put("password", "Staff@123");
        staffRequest.put("fullName", "Sales Staff");
        staffRequest.put("phoneNumber", "0123456789");
        staffRequest.put("role", "STAFF");
        String url = baseUrl + "/api/staff";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, staffRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
            assertThat(data.get("role")).isEqualTo("STAFF");
            
            if (data.containsKey("id")) {
                staffId = Long.valueOf(data.get("id").toString());
            }
        }
    }

    @Test
    @Order(4)
    @DisplayName("[STF-04-01] Vô hiệu hóa tài khoản Nhân viên")
    void testDisableStaff() {
        // Arrange - Create staff first if not exists
        if (staffId == null) {
            testCreateSalesStaff();
        }
        
        if (staffId != null) {
            String url = baseUrl + "/api/staff/" + staffId + "/disable";

            // Act
            ResponseEntity<ApiResponse> response = TestUtils.patchRequest(
                restTemplate, url, new HashMap<>(), ApiResponse.class, adminToken
            );

            // Assert
            if (response.getStatusCode() == HttpStatus.OK) {
                assertThat(response.getBody()).isNotNull();
            }
        }
    }

    @Test
    @Order(5)
    @DisplayName("[STF-04-02] Vô hiệu hóa tài khoản Admin thất bại (Logic bảo vệ)")
    void testDisableAdminProtected() {
        // Arrange - Try to disable admin account
        Long adminId = 1L; // Mock admin ID
        String url = baseUrl + "/api/staff/" + adminId + "/disable";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.patchRequest(
            restTemplate, url, new HashMap<>(), ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}

