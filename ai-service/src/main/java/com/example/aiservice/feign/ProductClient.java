package com.example.aiservice.feign;

import com.example.aiservice.response.ApiResponse;
import com.example.aiservice.response.PageResponse;
import com.example.aiservice.response.ProductColorResponse;
import com.example.aiservice.response.ProductResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@FeignClient(name = "product-service", contextId = "productClient")
public interface ProductClient {

    @GetMapping("/api/products/{id}")
    ApiResponse<ProductResponse> getProductById(@PathVariable("id") String id);

    @GetMapping("/api/product-colors/{id}")
    ApiResponse<ProductColorResponse> getProductColor(@PathVariable("id") String id);

    @GetMapping("/api/products")
    ApiResponse<List<ProductResponse>> getProducts();

    @GetMapping("/api/products/search")
    ApiResponse<PageResponse<ProductResponse>> searchProducts(
            @RequestParam(required = false) String request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size
    );
}
