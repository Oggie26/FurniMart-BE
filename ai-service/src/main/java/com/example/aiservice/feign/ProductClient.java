package com.example.aiservice.feign;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "product-service", contextId = "productClient")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("id") String id);

    @GetMapping("/api/products")
    ApiResponse<List<ProductResponse>> getProducts();

}
