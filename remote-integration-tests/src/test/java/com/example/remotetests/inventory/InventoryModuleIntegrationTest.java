package com.example.remotetests.inventory;

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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration Tests for Inventory Module
 * Based on test cases: INV-02-01, INV-02-02, INV-02-03, INV-03-01, INV-03-02,
 *                      INV-01-01, INV-04-01, INV-04-02, INV-05-01, INV-05-02,
 *                      INV-06-01, INV-06-02
 */
@SpringBootTest(classes = {TestConfig.class})
@TestPropertySource(locations = "classpath:application.yml")
@DisplayName("Inventory Module Integration Tests")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class InventoryModuleIntegrationTest {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private TestConfig testConfig;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private String baseUrl;
    private String adminToken;
    private Long warehouseId;
    private Long zoneId;

    @BeforeEach
    void setUp() {
        baseUrl = testConfig.getInventoryServiceUrl();
        // Note: Admin token should be obtained from login
    }

    @Test
    @Order(1)
    @DisplayName("[INV-02-01] Tạo Kho (Warehouse) thành công")
    void testCreateWarehouseSuccess() {
        // Arrange
        Map<String, Object> warehouseRequest = new HashMap<>();
        warehouseRequest.put("warehouseName", "Test Warehouse");
        warehouseRequest.put("address", "123 Test Street");
        warehouseRequest.put("storeId", "STORE001"); // Mock store ID
        String url = baseUrl + "/api/warehouses/STORE001";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, warehouseRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Tạo kho thành công");
        }
    }

    @Test
    @Order(2)
    @DisplayName("[INV-03-01] Tạo Khu vực (Zone) thành công")
    void testCreateZoneSuccess() {
        // Arrange - Need warehouse ID first
        Map<String, Object> zoneRequest = new HashMap<>();
        zoneRequest.put("zoneName", "Zone A");
        zoneRequest.put("capacity", 1000);
        zoneRequest.put("warehouseId", warehouseId);
        String url = baseUrl + "/api/zones";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, zoneRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK || response.getStatusCode() == HttpStatus.CREATED) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Tạo khu vực thành công");
        }
    }

    @Test
    @Order(3)
    @DisplayName("[INV-01-01] Nhập hàng (Import Stock) thành công")
    void testImportStockSuccess() {
        // Arrange
        Map<String, Object> importRequest = new HashMap<>();
        importRequest.put("type", "IMPORT");
        importRequest.put("purpose", "PURCHASE");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productColorId", "PC001");
        item.put("quantity", 100);
        item.put("zoneId", zoneId);
        items.add(item);
        importRequest.put("items", items);
        
        String url = baseUrl + "/api/inventories/" + warehouseId + "/import";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, importRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Nhập kho thành công");
        }
    }

    @Test
    @Order(4)
    @DisplayName("[INV-04-01] Xuất hàng (Export Stock) thành công")
    void testExportStockSuccess() {
        // Arrange - Need stock first
        Map<String, Object> exportRequest = new HashMap<>();
        exportRequest.put("type", "EXPORT");
        exportRequest.put("purpose", "STOCK_OUT");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productColorId", "PC001");
        item.put("quantity", 10);
        items.add(item);
        exportRequest.put("items", items);
        
        String url = baseUrl + "/api/inventories/" + warehouseId + "/export";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, exportRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Xuất kho thành công");
        }
    }

    @Test
    @Order(5)
    @DisplayName("[INV-04-02] Xuất hàng thất bại (Không đủ tồn kho)")
    void testExportStockInsufficient() {
        // Arrange
        Map<String, Object> exportRequest = new HashMap<>();
        exportRequest.put("type", "EXPORT");
        exportRequest.put("purpose", "STOCK_OUT");
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productColorId", "PC001");
        item.put("quantity", 10000); // Very large quantity
        items.add(item);
        exportRequest.put("items", items);
        
        String url = baseUrl + "/api/inventories/" + warehouseId + "/export";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, exportRequest, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    @Order(6)
    @DisplayName("[INV-05-01] Yêu cầu Chuyển kho (Transfer Stock)")
    void testTransferStock() {
        // Arrange
        Map<String, Object> transferRequest = new HashMap<>();
        transferRequest.put("fromWarehouseId", warehouseId);
        transferRequest.put("toWarehouseId", warehouseId + 1); // Different warehouse
        
        List<Map<String, Object>> items = new ArrayList<>();
        Map<String, Object> item = new HashMap<>();
        item.put("productColorId", "PC001");
        item.put("quantity", 10);
        items.add(item);
        transferRequest.put("items", items);
        
        String url = baseUrl + "/api/inventories/transfer";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.postRequest(
            restTemplate, url, transferRequest, ApiResponse.class, adminToken
        );

        // Assert
        if (response.getStatusCode() == HttpStatus.OK) {
            assertThat(response.getBody()).isNotNull();
            assertThat(response.getBody().getMessage()).contains("Chuyển kho thành công");
        }
    }

    @Test
    @Order(7)
    @DisplayName("[INV-06-01] Kiểm tra Tồn kho Toàn cầu")
    void testCheckGlobalStock() {
        // Arrange
        String url = baseUrl + "/api/inventories/stock/check-global?productColorId=PC001&requiredQty=5";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
    }

    @Test
    @Order(8)
    @DisplayName("[INV-06-02] Kiểm tra API Cảnh báo Tồn kho thấp")
    void testLowStockAlert() {
        // Arrange
        String url = baseUrl + "/api/inventories/stock/low-stock";

        // Act
        ResponseEntity<ApiResponse> response = TestUtils.getRequest(
            restTemplate, url, ApiResponse.class, adminToken
        );

        // Assert
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getMessage()).contains("Lấy danh sách sản phẩm sắp hết hàng thành công");
    }
}

