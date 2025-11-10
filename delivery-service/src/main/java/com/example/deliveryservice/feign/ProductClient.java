package com.example.deliveryservice.feign;

import com.example.deliveryservice.response.ApiResponse;
import com.example.deliveryservice.response.ProductColorResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "product-service", contextId = "productClient")
public interface ProductClient {

    @GetMapping("/api/product-colors/{id}")
    ApiResponse<ProductColorResponse> getProductColor(@PathVariable("id") String id);
}

