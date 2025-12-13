package com.example.orderservice.service;

import com.example.orderservice.feign.ProductClient;
import com.example.orderservice.response.ApiResponse;
import com.example.orderservice.response.ProductColorResponse;
import com.example.orderservice.response.ProductResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductServiceClient {

    private final ProductClient productClient;

    @Cacheable(value = "product-colors", key = "#id")
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

    @Cacheable(value = "products", key = "#id")
    public ProductResponse getProductById(String id) {
        try {
            ApiResponse<ProductResponse> response = productClient.getProductById(id);
            if (response != null && response.getData() != null) {
                return response.getData();
            }
        } catch (Exception e) {
            log.warn("Error fetching product for id {} from Product Service: {}", id, e.getMessage());
        }
        return null; // Handle null gracefully in caller
    }
}
