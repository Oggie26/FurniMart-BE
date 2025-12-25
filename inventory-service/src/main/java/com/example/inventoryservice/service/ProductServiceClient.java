package com.example.inventoryservice.service;

import com.example.inventoryservice.feign.ProductClient;
import com.example.inventoryservice.response.ApiResponse;
import com.example.inventoryservice.response.ProductColorResponse;
import com.example.inventoryservice.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final ProductClient productClient;

    public ProductColorResponse getProductColor(String id) {
        try {
            ApiResponse<ProductColorResponse> response = productClient.getProductColor(id);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching product color for id {} from Product Service: {}", id, e.getMessage());
        }
        return null;
    }

    public ProductResponse getProductById(String id) {
        try {
            ApiResponse<ProductResponse> response = productClient.getProductById(id);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching product for id {} from Product Service: {}", id, e.getMessage());
        }
        return null;
    }
}
