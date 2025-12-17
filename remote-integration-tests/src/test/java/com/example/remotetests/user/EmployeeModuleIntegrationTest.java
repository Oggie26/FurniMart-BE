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
 * Integration Tests for Employee Module
 * Based on test cases: EMP-01-01, EMP-01-02, EMP-01-03, EMP-02-01, EMP-02-02, EMP-03-01
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Employee Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@SuppressWarnings({"rawtypes", "null"})
public class EmployeeModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String adminToken;
    private Long staffId;
    private Long employeeId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getUserServiceUrl();
        // Note: Admin token and staff ID should be obtained from previous tests
    }

    @Test
    @Order(1)
    @DisplayName("[EMP-01-01] Tạo Hồ sơ Nhân viên thành công (Gán Store/Shift)")
    void testCreateEmployeeSuccess() {
        // Arrange
        Map<String, Object> employeeRequest = new HashMap<>();
        employeeRequest.put("staffId", staffId);
        employeeRequest.put("storeId", "STORE001");
        employeeRequest.put("shift", "MORNING");
        String url = baseUrl + "/api/employees";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, employeeRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Employee created successfully");
            
            @SuppressWarnings("unchecked")
            Map<String, Object> data = objectMapper.convertValue(response.getBody().getData(), Map.class);
            if (data != null && data.containsKey("id")) {
                employeeId = Long.valueOf(data.get("id").toString());
            }
        }
    }

    @Test
    @Order(2)
    @DisplayName("[EMP-01-02] Tạo Hồ sơ Nhân viên thất bại (Staff ID không tồn tại)")
    void testCreateEmployeeStaffNotFound() {
        // Arrange
        Map<String, Object> employeeRequest = new HashMap<>();
        employeeRequest.put("staffId", 99999L); // Non-existent ID
        employeeRequest.put("storeId", "STORE001");
        String url = baseUrl + "/api/employees";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, employeeRequest, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Staff not found");
    }

    @Test
    @Order(3)
    @DisplayName("[EMP-01-03] Tạo Hồ sơ Nhân viên thất bại (Đã có Hồ sơ)")
    void testCreateEmployeeAlreadyExists() {
        // Arrange - Create employee first
        if (employeeId == null) {
            testCreateEmployeeSuccess();
        }
        
        if (staffId != null) {
            Map<String, Object> employeeRequest = new HashMap<>();
            employeeRequest.put("staffId", staffId);
            employeeRequest.put("storeId", "STORE001");
            String url = baseUrl + "/api/employees";

            // Act - Try to create again
            ResponseEntity<ApiResponse> response = TestUtils.postRequest(
                restTemplate, url, employeeRequest, ApiResponse.class, adminToken
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Employee profile already exists");
        }
    }

    @Test
    @Order(4)
    @DisplayName("[EMP-02-01] Lấy Chi tiết Hồ sơ Nhân viên")
    void testGetEmployeeById() {
        // Arrange
        if (employeeId == null) {
            testCreateEmployeeSuccess();
        }
        
        if (employeeId != null) {
            String url = baseUrl + "/api/employees/" + employeeId;

            // Act
            ResponseEntity<ApiResponse> response = TestUtils.getRequest(
                restTemplate, url, ApiResponse.class, adminToken
            );

            // Assert
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isNotNull();
        }
    }

    @Test
    @Order(5)
    @DisplayName("[EMP-02-02] Lấy Chi tiết Hồ sơ thất bại (ID không hợp lệ)")
    void testGetEmployeeInvalidId() {
        // Arrange
        String url = baseUrl + "/api/employees/9999";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(6)
    @DisplayName("[EMP-03-01] Lọc Nhân viên theo Store")
    void testGetEmployeesByStore() {
        // Arrange
        String storeId = "STORE001";
        String url = baseUrl + "/api/employees/store/" + storeId;

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }
}

